import assert from 'node:assert/strict';
import test from 'node:test';
import { buildGuardianApp, type GuardianOperations } from './http.js';

const token = 'a'.repeat(64);
const authorization = { authorization: `Bearer ${token}` };

function fakeOperations() {
  const calls = {
    start: 0,
    stop: 0,
    restart: 0,
    lockdown: [] as boolean[]
  };
  const operations: GuardianOperations = {
    status: async () => ({ state: 'stopped' }),
    logs: () => [],
    start: () => { calls.start += 1; },
    stop: async () => { calls.stop += 1; },
    restart: async () => { calls.restart += 1; },
    setLockdown: async enabled => { calls.lockdown.push(enabled); }
  };
  return { operations, calls };
}

test('API routes require the bearer token while dashboard paths remain public', async t => {
  const { operations } = fakeOperations();
  const app = await buildGuardianApp({ token, operations });
  t.after(async () => { await app.close(); });

  const unauthorized = await app.inject({ method: 'GET', url: '/v1/status' });
  assert.equal(unauthorized.statusCode, 401);

  const authorized = await app.inject({
    method: 'GET',
    url: '/v1/status',
    headers: authorization
  });
  assert.equal(authorized.statusCode, 200);

  const dashboardWithQuery = await app.inject({ method: 'GET', url: '/?source=test' });
  assert.notEqual(dashboardWithQuery.statusCode, 401);
});

test('control routes share one six-request rate-limit bucket', async t => {
  const { operations } = fakeOperations();
  const app = await buildGuardianApp({ token, operations });
  t.after(async () => { await app.close(); });

  const requests = [
    { url: '/v1/router/start' },
    { url: '/v1/router/stop' },
    { url: '/v1/router/restart' },
    { url: '/v1/lockdown', payload: { enabled: true } },
    { url: '/v1/router/start' },
    { url: '/v1/router/stop' }
  ];

  for (const request of requests) {
    const response = await app.inject({
      method: 'POST',
      url: request.url,
      headers: authorization,
      payload: request.payload
    });
    assert.equal(response.statusCode, 200);
  }

  const limited = await app.inject({
    method: 'POST',
    url: '/v1/router/restart',
    headers: authorization
  });
  assert.equal(limited.statusCode, 429);
});

test('status and logs do not consume the control-operation quota', async t => {
  const { operations } = fakeOperations();
  const app = await buildGuardianApp({ token, operations });
  t.after(async () => { await app.close(); });

  for (let index = 0; index < 12; index += 1) {
    const statusResponse = await app.inject({ method: 'GET', url: '/v1/status', headers: authorization });
    const logsResponse = await app.inject({ method: 'GET', url: '/v1/logs', headers: authorization });
    assert.equal(statusResponse.statusCode, 200);
    assert.equal(logsResponse.statusCode, 200);
  }

  for (let index = 0; index < 6; index += 1) {
    const response = await app.inject({ method: 'POST', url: '/v1/router/start', headers: authorization });
    assert.equal(response.statusCode, 200);
  }

  const limited = await app.inject({ method: 'POST', url: '/v1/router/start', headers: authorization });
  assert.equal(limited.statusCode, 429);
});

test('lockdown accepts an explicit disable operation', async t => {
  const { operations, calls } = fakeOperations();
  const app = await buildGuardianApp({ token, operations });
  t.after(async () => { await app.close(); });

  const response = await app.inject({
    method: 'POST',
    url: '/v1/lockdown',
    headers: authorization,
    payload: { enabled: false }
  });

  assert.equal(response.statusCode, 200);
  assert.deepEqual(calls.lockdown, [false]);
});
