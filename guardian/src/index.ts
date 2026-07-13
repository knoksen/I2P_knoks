import Fastify from 'fastify';
import fastifyStatic from '@fastify/static';
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { randomBytes, timingSafeEqual } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { homedir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import net from 'node:net';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');
const dataDir = process.env.GUARDIAN_DATA_DIR ?? join(homedir(), '.i2p-knoks');
const configPath = join(dataDir, 'guardian.json');

type State = 'stopped' | 'starting' | 'connected' | 'degraded' | 'stopping' | 'failed';
type Config = {
  host: string; port: number; i2pdBinary: string; i2pdArgs: string[];
  routerConsolePort: number; httpProxyPort: number; socksProxyPort: number; samPort: number;
  autostart: boolean; restartOnFailure: boolean; maxRestarts: number; healthIntervalMs: number;
};

const defaults: Config = {
  host: '127.0.0.1', port: 17656, i2pdBinary: process.env.I2PD_BINARY ?? 'i2pd',
  i2pdArgs: ['--conf', join(dataDir, 'i2pd.conf')], routerConsolePort: 7070,
  httpProxyPort: 4444, socksProxyPort: 4447, samPort: 7656,
  autostart: true, restartOnFailure: true, maxRestarts: 5, healthIntervalMs: 5000
};

let config: Config = defaults;
let child: ChildProcessWithoutNullStreams | undefined;
let state: State = 'stopped';
let startedAt: number | undefined;
let restarts = 0;
let lastError: string | undefined;
let lockdown = false;
const logs: { at: string; level: string; message: string }[] = [];

function log(level: string, message: string) {
  const entry = { at: new Date().toISOString(), level, message };
  logs.push(entry); if (logs.length > 500) logs.shift();
  console.log(JSON.stringify(entry));
}
async function loadConfig() {
  await mkdir(dataDir, { recursive: true });
  try { config = { ...defaults, ...JSON.parse(await readFile(configPath, 'utf8')) }; }
  catch { await writeFile(configPath, JSON.stringify(defaults, null, 2)); }
}
function tcpCheck(port: number, timeout = 1200): Promise<boolean> {
  return new Promise(resolveCheck => {
    const socket = net.createConnection({ host: '127.0.0.1', port });
    const done = (ok: boolean) => { socket.destroy(); resolveCheck(ok); };
    socket.setTimeout(timeout); socket.once('connect', () => done(true));
    socket.once('timeout', () => done(false)); socket.once('error', () => done(false));
  });
}
async function status() {
  const [consoleUp, httpUp, socksUp, samUp] = await Promise.all([
    tcpCheck(config.routerConsolePort), tcpCheck(config.httpProxyPort),
    tcpCheck(config.socksProxyPort), tcpCheck(config.samPort)
  ]);
  if (child && state !== 'starting' && state !== 'stopping') state = consoleUp ? 'connected' : 'degraded';
  return {
    state, pid: child?.pid, uptimeSeconds: startedAt ? Math.floor((Date.now() - startedAt) / 1000) : 0,
    restarts, lockdown, lastError,
    endpoints: { console: consoleUp, httpProxy: httpUp, socksProxy: socksUp, sam: samUp },
    addresses: { console: `http://127.0.0.1:${config.routerConsolePort}`, httpProxy: `127.0.0.1:${config.httpProxyPort}`, socksProxy: `127.0.0.1:${config.socksProxyPort}`, sam: `127.0.0.1:${config.samPort}` }
  };
}
function startRouter() {
  if (child || lockdown) return;
  state = 'starting'; lastError = undefined;
  log('info', `Starting ${config.i2pdBinary}`);
  child = spawn(config.i2pdBinary, config.i2pdArgs, { stdio: 'pipe', windowsHide: true });
  startedAt = Date.now();
  child.stdout.on('data', b => log('info', b.toString().trim()));
  child.stderr.on('data', b => log('warn', b.toString().trim()));
  child.once('error', error => { lastError = error.message; state = 'failed'; log('error', error.message); child = undefined; });
  child.once('exit', (code, signal) => {
    child = undefined; startedAt = undefined; state = 'stopped';
    log(code === 0 ? 'info' : 'warn', `i2pd exited code=${code} signal=${signal}`);
    if (!lockdown && config.restartOnFailure && code !== 0 && restarts < config.maxRestarts) {
      restarts++; setTimeout(startRouter, Math.min(30_000, 1000 * 2 ** restarts));
    }
  });
}
async function stopRouter() {
  if (!child) { state = 'stopped'; return; }
  state = 'stopping'; const active = child; active.kill('SIGTERM');
  await new Promise<void>(resolveStop => {
    const timeout = setTimeout(() => { active.kill('SIGKILL'); resolveStop(); }, 8000);
    active.once('exit', () => { clearTimeout(timeout); resolveStop(); });
  });
}
async function main() {
  await loadConfig();
  const tokenPath = join(dataDir, 'api-token');
  let token: string;
  try { token = (await readFile(tokenPath, 'utf8')).trim(); }
  catch { token = randomBytes(32).toString('hex'); await writeFile(tokenPath, token, { mode: 0o600 }); }

  const app = Fastify({ logger: false, bodyLimit: 32_768 });
  app.addHook('onRequest', async request => {
    if (request.url === '/' || request.url.startsWith('/assets')) return;
    const supplied = request.headers.authorization?.replace(/^Bearer\s+/i, '') ?? '';
    const a = Buffer.from(supplied); const b = Buffer.from(token);
    if (a.length !== b.length || !timingSafeEqual(a, b)) throw app.httpErrors?.unauthorized?.() ?? new Error('Unauthorized');
  });
  await app.register(fastifyStatic, { root: join(root, 'public'), prefix: '/' });
  app.get('/v1/status', status);
  app.get('/v1/logs', async () => ({ logs: logs.slice(-200) }));
  app.post('/v1/router/start', async () => { startRouter(); return status(); });
  app.post('/v1/router/stop', async () => { await stopRouter(); return status(); });
  app.post('/v1/router/restart', async () => { await stopRouter(); restarts = 0; startRouter(); return status(); });
  app.post('/v1/lockdown', async request => {
    const body = request.body as { enabled?: boolean } | undefined; lockdown = body?.enabled !== false;
    if (lockdown) await stopRouter(); return status();
  });
  app.get('/v1/bootstrap', async () => ({ token, dataDir }));
  await app.listen({ host: config.host, port: config.port });
  log('info', `Guardian listening on http://${config.host}:${config.port}`);
  setInterval(() => void status(), config.healthIntervalMs).unref();
  if (config.autostart) startRouter();
  const shutdown = async () => { await stopRouter(); await app.close(); process.exit(0); };
  process.on('SIGINT', shutdown); process.on('SIGTERM', shutdown);
}
main().catch(error => { console.error(error); process.exit(1); });
