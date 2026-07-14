# I2P Knoks Guardian

Local-first background supervisor for `i2pd`, with a loopback-only API and browser dashboard.

## Implemented in v0.1

- Starts, stops and restarts an `i2pd` child process.
- Automatic restart with bounded exponential backoff.
- Intent-aware shutdown, so a manual stop does not trigger an automatic restart.
- Restart counter reset after ten minutes of stable operation.
- TCP health checks for router console, HTTP proxy, SOCKS proxy and SAM.
- Local dashboard at `http://127.0.0.1:17656` by default.
- 256-bit bearer token stored in the Guardian data directory.
- Lockdown action that stops the router and blocks restart.
- Bounded in-memory operational logs.
- Validated configuration stored in the Guardian data directory.
- Shared rate limit for process-changing control operations.

## Prerequisites

- Node.js 20+
- `i2pd` installed and available on `PATH`, or `I2PD_BINARY` set to an absolute path ending in `i2pd` or `i2pd.exe`.

## Run

```bash
cd guardian
npm install
npm run check
npm run build
npm start
```

Open `http://127.0.0.1:17656` and paste the API token.

Default token locations:

- Linux/macOS: `cat ~/.i2p-knoks/api-token`
- PowerShell: `Get-Content "$HOME/.i2p-knoks/api-token"`

When `GUARDIAN_DATA_DIR` is set, both `api-token` and `guardian.json` are stored directly under that directory instead. The override must be an absolute, non-root path.

## Configuration

The first run creates `guardian.json`. Invalid JSON or invalid values stop startup with an error; the existing configuration is never silently overwritten.

The Guardian API bind address and the monitored router address are independently configurable through `host` and `routerHost`, but both are restricted to literal loopback addresses:

- `127.0.0.1`
- `::1`

This enforcement prevents an accidental configuration edit from exposing process-control routes to an external network.

Typical i2pd configuration:

```ini
[http]
enabled = true
address = 127.0.0.1
port = 7070

[httpproxy]
enabled = true
address = 127.0.0.1
port = 4444

[socksproxy]
enabled = true
address = 127.0.0.1
port = 4447

[sam]
enabled = true
address = 127.0.0.1
port = 7656
```

Set `routerHost` to `::1` only when the corresponding i2pd listeners are also bound to IPv6 loopback.

## Security boundary

This is an engineering MVP, not a guarantee of anonymity or production readiness. Do not expose the dashboard, router console, SAM, I2CP, proxy ports, API token, private keys or destination keys to untrusted networks. The dashboard intentionally performs no remote-control discovery and telemetry is disabled.

Authenticated process-control routes share a limit of six operations per minute per client. Status polling, log reads and dashboard assets are not included in that control quota.

The executable name is constrained to `i2pd` or `i2pd.exe`, arguments are constructed internally, and `spawn` runs with `shell: false`.

## Planned next stage

- Tauri system-tray shell.
- Windows Service and systemd installers.
- Signed release artifacts and update verification.
- OS keychain-backed token storage.
- Tunnel profile management and destination-key backup controls.
- Integration and fault-injection tests using a mock router.
