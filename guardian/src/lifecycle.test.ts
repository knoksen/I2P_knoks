import assert from 'node:assert/strict';
import test from 'node:test';
import { deriveHealthState, shouldRestartRouter } from './lifecycle.js';

const restartable = {
  shouldRun: true,
  lockdown: false,
  state: 'connected' as const,
  restartOnFailure: true,
  restarts: 1,
  maxRestarts: 5
};

test('an unexpected exit is eligible for automatic restart', () => {
  assert.equal(shouldRestartRouter(restartable), true);
});

test('a manual stop never schedules an automatic restart', () => {
  assert.equal(shouldRestartRouter({ ...restartable, state: 'stopping' }), false);
  assert.equal(shouldRestartRouter({ ...restartable, shouldRun: false }), false);
});

test('lockdown and the restart ceiling block automatic restart', () => {
  assert.equal(shouldRestartRouter({ ...restartable, lockdown: true }), false);
  assert.equal(shouldRestartRouter({ ...restartable, restarts: 5 }), false);
});

test('health checks transition startup to connected or degraded', () => {
  assert.equal(deriveHealthState(true, 1000, 10_000), 'connected');
  assert.equal(deriveHealthState(false, 1000, 10_000), 'starting');
  assert.equal(deriveHealthState(false, 10_000, 10_000), 'degraded');
});
