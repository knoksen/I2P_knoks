export type State = 'stopped' | 'starting' | 'connected' | 'degraded' | 'stopping' | 'failed';

type RestartDecision = {
  shouldRun: boolean;
  lockdown: boolean;
  state: State;
  restartOnFailure: boolean;
  restarts: number;
  maxRestarts: number;
};

export function shouldRestartRouter(input: RestartDecision): boolean {
  return input.shouldRun
    && !input.lockdown
    && input.state !== 'stopping'
    && input.restartOnFailure
    && input.restarts < input.maxRestarts;
}

export function deriveHealthState(
  consoleUp: boolean,
  elapsedMs: number,
  startupGraceMs: number
): Extract<State, 'starting' | 'connected' | 'degraded'> {
  if (consoleUp) return 'connected';
  return elapsedMs < startupGraceMs ? 'starting' : 'degraded';
}
