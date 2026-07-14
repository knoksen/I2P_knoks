import Fastify from 'fastify';
import rateLimit from '@fastify/rate-limit';
import fastifyStatic from '@fastify/static';
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { randomBytes, timingSafeEqual } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { homedir } from 'node:os';
import { basename, dirname, isAbsolute, join, normalize, parse, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import net from 'node:net';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');
const LOOPBACK_HOSTS = new Set(['127.0.0.1', '::1']);
const RESTART_RESET_MS = 10 * 60_000;

type LoopbackHost = '127.0.0.1' | '::1';
type State = 'stopped' | 'starting' | 'connected' | 'degraded' | 'stopping' | 'failed';
type Config = {
  host: LoopbackHost;
  routerHost: LoopbackHost;
  port: number;
  routerConsolePort: number;
  httpProxyPort: number;
  socksProxyPort: number;
  samPort: number;
  autostart: boolean;
  restartOnFailure: boolean;
  maxRestarts: number;
  healthIntervalMs: number;
};

function resolveDataDir(): string {
  const configured = process.env.GUARDIAN_DATA_DIR?.trim();
  if (!configured) return join(homedir(), '.i2p-knoks');
  if (configured.includes('\0') || !isAbsolute(configured)) {
    throw new Error('GUARDIAN_DATA_DIR must be an absolute path without null bytes');
  }
  const normalized = normalize(configured);
  if (normalized === parse(normalized).root) {
    throw new Error('GUARDIAN_DATA_DIR must not be the filesystem root');
  }
  return normalized;
}

function resolveI2pdBinary(): string {
  const configured = (process.env.I2PD_BINARY ?? 'i2pd').trim();
  if (!configured || configured.includes('\0')) throw new Error('I2PD_BINARY is invalid');
  const executable = basename(configured).toLowerCase();
  if (executable !== 'i2pd' && executable !== 'i2pd.exe') {
    throw new Error('I2PD_BINARY must point to an i2pd or i2pd.exe executable');
  }
  if (!isAbsolute(configured) && configured !== 'i2pd' && configured !== 'i2pd.exe') {
    throw new Error('I2PD_BINARY must be an absolute path or a PATH-resolved executable name');
  }
  return configured;
}

const dataDir = resolveDataDir();
const configPath = join(dataDir, 'guardian.json');
const tokenPath = join(dataDir, 'api-token');
const i2pdConfigPath = join(dataDir, 'i2pd.conf');
const i2pdBinary = resolveI2pdBinary();

const defaults: Config = {
  host: '127.0.0.1',
  routerHost: '127.0.0.1',
  port: 17656,
  routerConsolePort: 7070,
  httpProxyPort: 4444,
  socksProxyPort: 4447,
  samPort: 7656,
  autostart: true,
  restartOnFailure: true,
  maxRestarts: 5,
  healthIntervalMs: 5000
};

let config: Config = defaults;
let child: ChildProcessWithoutNullStreams | undefined;
let state: State = 'stopped';
let startedAt: number | undefined;
let restarts = 0;
let lastError: string | undefined;
let lockdown = false;
let shouldRun = false;
let restartTimer: NodeJS.Timeout | undefined;
let stableTimer: NodeJS.Timeout | undefined;
let shuttingDown = false;
const logs: { at: string; level: string; message: string }[] = [];

function log(level: string, message: string) {
  const entry = { at: new Date().toISOString(), level, message };
  logs.push(entry);
  if (logs.length > 500) logs.shift();
  console.log(JSON.stringify(entry));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function requireLoopbackHost(value: unknown, name: string): LoopbackHost {
  if (typeof value !== 'string' || !LOOPBACK_HOSTS.has(value)) {
    throw new Error(`${name} must be 127.0.0.1 or ::1`);
  }
  return value as LoopbackHost;
}

function requireInteger(value: unknown, name: string, min: number, max: number): number {
  if (typeof value !== 'number' || !Number.isInteger(value) || value < min || value > max) {
    throw new Error(`${name} must be an integer between ${min} and ${max}`);
  }
  return value;
}

function requireBoolean(value: unknown, name: string): boolean {
  if (typeof value !== 'boolean') throw new Error(`${name} must be a boolean`);
  return value;
}

function parseConfig(value: unknown): Config {
  if (!isRecord(value)) throw new Error('guardian.json must contain a JSON object');
  const candidate: Record<string, unknown> = { ...defaults, ...value };
  return {
    host: requireLoopbackHost(candidate.host, 'host'),
    routerHost: requireLoopbackHost(candidate.routerHost, 'routerHost'),
    port: requireInteger(candidate.port, 'port', 1, 65_535),
    routerConsolePort: requireInteger(candidate.routerConsolePort, 'routerConsolePort', 1, 65_535),
    httpProxyPort: requireInteger(candidate.httpProxyPort, 'httpProxyPort', 1, 65_535),
    socksProxyPort: requireInteger(candidate.socksProxyPort, 'socksProxyPort', 1, 65_535),
    samPort: requireInteger(candidate.samPort, 'samPort', 1, 65_535),
    autostart: requireBoolean(candidate.autostart, 'autostart'),
    restartOnFailure: requireBoolean(candidate.restartOnFailure, 'restartOnFailure'),
    maxRestarts: requireInteger(candidate.maxRestarts, 'maxRestarts', 0, 100),
    healthIntervalMs: requireInteger(candidate.healthIntervalMs, 'healthIntervalMs', 1000, 60_000)
  };
}

function errorCode(error: unknown): string | undefined {
  return isRecord(error) && typeof error.code === 'string' ? error.code : undefined;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

async function loadConfig() {
  await mkdir(dataDir, { recursive: true, mode: 0o700 });
  let raw: string;
  try {
    raw = await readFile(configPath, 'utf8');
  } catch (error) {
    if (errorCode(error) !== 'ENOENT') throw error;
    config = defaults;
    await writeFile(configPath, JSON.stringify(defaults, null, 2), {
      encoding: 'utf8', mode: 0o600, flag: 'wx'
    });
    return;
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw) as unknown;
  } catch (error) {
    throw new Error(`Invalid JSON in ${configPath}: ${errorMessage(error)}`);
  }
  config = parseConfig(parsed);
}

async function loadToken(): Promise<string> {
  try {
    const token = (await readFile(tokenPath, 'utf8')).trim();
    if (!/^[a-f0-9]{64}$/i.test(token)) throw new Error(`Invalid API token in ${tokenPath}`);
    return token;
  } catch (error) {
    if (errorCode(error) !== 'ENOENT') throw error;
    const token = randomBytes(32).toString('hex');
    await writeFile(tokenPath, token, { encoding: 'utf8', mode: 0o600, flag: 'wx' });
    return token;
  }
}

function clearRestartTimer() {
  if (!restartTimer) return;
  clearTimeout(restartTimer);
  restartTimer = undefined;
}

function clearStableTimer() {
  if (!stableTimer) return;
  clearTimeout(stableTimer);
  stableTimer = undefined;
}

function formatHostPort(host: LoopbackHost, port: number): string {
  return host === '::1' ? `[${host}]:${port}` : `${host}:${port}`;
}

function tcpCheck(host: LoopbackHost, port: number, timeout = 1200): Promise<boolean> {
  return new Promise(resolveCheck => {
    const socket = net.createConnection({ host, port });
    const done = (ok: boolean) => {
      socket.destroy();
      resolveCheck(ok);
    };
    socket.setTimeout(timeout);
    socket.once('connect', () => { done(true); });
    socket.once('timeout', () => { done(false); });
    socket.once('error', () => { done(false); });
  });
}

async function status() {
  const [consoleUp, httpUp, socksUp, samUp] = await Promise.all([
    tcpCheck(config.routerHost, config.routerConsolePort),
    tcpCheck(config.routerHost, config.httpProxyPort),
    tcpCheck(config.routerHost, config.socksProxyPort),
    tcpCheck(config.routerHost, config.samPort)
  ]);
  if (child && state !== 'stopping') {
    const startupGraceMs = Math.max(10_000, config.healthIntervalMs * 2);
    const withinStartupGrace = startedAt !== undefined && Date.now() - startedAt < startupGraceMs;
    state = consoleUp ? 'connected' : withinStartupGrace ? 'starting' : 'degraded';
  }
  const routerAddress = (port: number) => formatHostPort(config.routerHost, port);
  return {
    state,
    pid: child?.pid,
    uptimeSeconds: startedAt ? Math.floor((Date.now() - startedAt) / 1000) : 0,
    restarts,
    lockdown,
    lastError,
    endpoints: { console: consoleUp, httpProxy: httpUp, socksProxy: socksUp, sam: samUp },
    addresses: {
      console: `http://${routerAddress(config.routerConsolePort)}`,
      httpProxy: routerAddress(config.httpProxyPort),
      socksProxy: routerAddress(config.socksProxyPort),
      sam: routerAddress(config.samPort)
    }
  };
}

function scheduleRestart() {
  if (!shouldRun || lockdown || !config.restartOnFailure || restarts >= config.maxRestarts) return;
  restarts += 1;
  const delay = Math.min(30_000, 1000 * 2 ** restarts);
  log('warn', `Scheduling i2pd restart ${restarts}/${config.maxRestarts} in ${delay}ms`);
  restartTimer = setTimeout(() => {
    restartTimer = undefined;
    startRouter();
  }, delay);
  restartTimer.unref();
}

function startRouter() {
  if (lockdown) return;
  shouldRun = true;
  if (child) return;
  clearRestartTimer();
  state = 'starting';
  lastError = undefined;
  log('info', `Starting ${i2pdBinary}`);

  const active = spawn(i2pdBinary, ['--conf', i2pdConfigPath], {
    stdio: 'pipe', windowsHide: true, shell: false
  });
  child = active;
  startedAt = Date.now();
  stableTimer = setTimeout(() => {
    if (child === active && shouldRun && !lockdown) {
      restarts = 0;
      log('info', 'i2pd restart counter reset after stable operation');
    }
  }, RESTART_RESET_MS);
  stableTimer.unref();

  active.stdout.on('data', data => { log('info', data.toString().trim()); });
  active.stderr.on('data', data => { log('warn', data.toString().trim()); });

  let finalized = false;
  const finalize = (code: number | null, signal: NodeJS.Signals | null, error?: Error) => {
    if (finalized) return;
    finalized = true;
    clearStableTimer();
    const intentional = !shouldRun || lockdown || state === 'stopping';
    if (child === active) child = undefined;
    startedAt = undefined;

    if (error) {
      lastError = error.message;
      log('error', error.message);
    } else {
      const message = `i2pd exited code=${code} signal=${signal}`;
      if (!intentional) lastError = message;
      log(intentional || code === 0 ? 'info' : 'warn', message);
    }

    state = intentional ? 'stopped' : 'failed';
    if (!intentional) scheduleRestart();
  };

  active.once('error', error => { finalize(null, null, error); });
  active.once('exit', (code, signal) => { finalize(code, signal); });
}

async function stopRouter() {
  shouldRun = false;
  clearRestartTimer();
  clearStableTimer();
  if (!child) {
    state = 'stopped';
    return;
  }

  state = 'stopping';
  const active = child;
  await new Promise<void>(resolveStop => {
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      resolveStop();
    };
    const timeout = setTimeout(() => {
      active.kill('SIGKILL');
      finish();
    }, 8000);
    active.once('exit', () => { finish(); });
    if (!active.kill('SIGTERM')) finish();
  });
}

function isPublicDashboardRequest(method: string, url: string): boolean {
  if (method !== 'GET' && method !== 'HEAD') return false;
  const queryIndex = url.indexOf('?');
  const pathname = queryIndex >= 0 ? url.slice(0, queryIndex) : url;
  return pathname === '/' || pathname === '/index.html' || pathname === '/favicon.ico' || pathname.startsWith('/assets/');
}

async function main() {
  await loadConfig();
  const token = await loadToken();
  const app = Fastify({ logger: false, bodyLimit: 32_768 });

  app.addHook('onRequest', async (request, reply) => {
    if (isPublicDashboardRequest(request.method, request.url)) return;
    const supplied = request.headers.authorization?.replace(/^Bearer\s+/i, '') ?? '';
    const suppliedBuffer = Buffer.from(supplied);
    const tokenBuffer = Buffer.from(token);
    if (suppliedBuffer.length !== tokenBuffer.length || !timingSafeEqual(suppliedBuffer, tokenBuffer)) {
      await reply.code(401).send({ error: 'Unauthorized' });
    }
  });

  await app.register(rateLimit, { global: false, hook: 'preHandler' });
  await app.register(fastifyStatic, { root: join(root, 'public'), prefix: '/' });
  app.get('/v1/status', status);
  app.get('/v1/logs', async () => ({ logs: logs.slice(-200) }));
  app.post('/v1/router/start', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    startRouter();
    return status();
  });
  app.post('/v1/router/stop', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    await stopRouter();
    return status();
  });
  app.post('/v1/router/restart', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    await stopRouter();
    restarts = 0;
    startRouter();
    return status();
  });
  app.post('/v1/lockdown', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async request => {
    const body = request.body as { enabled?: boolean } | undefined;
    lockdown = body?.enabled !== false;
    if (lockdown) await stopRouter();
    return status();
  });

  await app.listen({ host: config.host, port: config.port });
  log('info', `Guardian listening on http://${formatHostPort(config.host, config.port)}`);
  setInterval(() => { void status(); }, config.healthIntervalMs).unref();
  if (config.autostart) startRouter();

  const shutdown = async (signal: NodeJS.Signals) => {
    if (shuttingDown) return;
    shuttingDown = true;
    log('info', `Received ${signal}; shutting down Guardian`);
    try {
      await stopRouter();
      await app.close();
      process.exitCode = 0;
    } catch (error) {
      console.error(error);
      process.exitCode = 1;
    }
  };

  process.once('SIGINT', async () => { await shutdown('SIGINT'); });
  process.once('SIGTERM', async () => { await shutdown('SIGTERM'); });
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
