import Fastify, { type FastifyInstance } from 'fastify';
import rateLimit from '@fastify/rate-limit';
import fastifyStatic from '@fastify/static';
import { timingSafeEqual } from 'node:crypto';

export type GuardianLog = { at: string; level: string; message: string };

export interface GuardianOperations {
  status(): Promise<unknown>;
  logs(): GuardianLog[];
  start(): void | Promise<void>;
  stop(): Promise<void>;
  restart(): Promise<void>;
  setLockdown(enabled: boolean): Promise<void>;
}

type BuildGuardianAppOptions = {
  token: string;
  operations: GuardianOperations;
  publicRoot?: string;
};

function isPublicDashboardRequest(method: string, url: string): boolean {
  if (method !== 'GET' && method !== 'HEAD') return false;
  const queryIndex = url.indexOf('?');
  const pathname = queryIndex >= 0 ? url.slice(0, queryIndex) : url;
  return pathname === '/' || pathname === '/index.html' || pathname === '/favicon.ico' || pathname.startsWith('/assets/');
}

function authorized(supplied: string, expected: string): boolean {
  const suppliedBuffer = Buffer.from(supplied);
  const expectedBuffer = Buffer.from(expected);
  return suppliedBuffer.length === expectedBuffer.length && timingSafeEqual(suppliedBuffer, expectedBuffer);
}

export async function buildGuardianApp(options: BuildGuardianAppOptions): Promise<FastifyInstance> {
  const app = Fastify({ logger: false, bodyLimit: 32_768 });

  app.addHook('onRequest', async (request, reply) => {
    if (isPublicDashboardRequest(request.method, request.url)) return;
    const supplied = request.headers.authorization?.replace(/^Bearer\s+/i, '') ?? '';
    if (!authorized(supplied, options.token)) {
      await reply.code(401).send({ error: 'Unauthorized' });
    }
  });

  await app.register(rateLimit, { global: false, hook: 'preHandler' });
  if (options.publicRoot) {
    await app.register(fastifyStatic, { root: options.publicRoot, prefix: '/' });
  }

  app.get('/v1/status', async () => options.operations.status());
  app.get('/v1/logs', async () => ({ logs: options.operations.logs().slice(-200) }));

  app.post('/v1/router/start', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    await options.operations.start();
    return options.operations.status();
  });

  app.post('/v1/router/stop', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    await options.operations.stop();
    return options.operations.status();
  });

  app.post('/v1/router/restart', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async () => {
    await options.operations.restart();
    return options.operations.status();
  });

  app.post('/v1/lockdown', {
    config: { rateLimit: { max: 6, timeWindow: '1 minute', groupId: 'guardian-control' } }
  }, async request => {
    const body = request.body as { enabled?: boolean } | undefined;
    await options.operations.setLockdown(body?.enabled !== false);
    return options.operations.status();
  });

  return app;
}
