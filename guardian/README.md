# I2P Knoks Guardian

Local-first background supervisor for `i2pd`, with a loopback-only API and browser dashboard.

## Implemented in v0.1

- Starts, stops and restarts an `i2pd` child process.
- Automatic restart with bounded exponential backoff.
- TCP health checks for router console, HTTP proxy, SOCKS proxy and SAM.
- Local dashboard at `http://127.0.0.1:17656`.
- 256-bit bearer token stored in `~/.i2p-knoks/api-token`.
- Lockdown action that stops the router and blocks restart.
- Bounded in-memory operational logs.
- Config generated at `~/.i2p-knoks/guardian.json`.

## Prerequisites

- Node.js 20+
- `i2pd` installed and available on `PATH`, or set `I2PD_BINARY`.

## Run

```bash
cd guardian
npm install
npm run check
npm run build
npm start
```

Open `http://127.0.0.1:17656` and paste the token from:

- Linux/macOS: `cat ~/.i2p-knoks/api-token`
- PowerShell: `Get-Content "$HOME/.i2p-knoks/api-token"`

## Configuration

The first run creates `guardian.json`. All management and proxy endpoints must remain bound to loopback unless a deployment-specific threat model, authentication layer and firewall policy explicitly justify otherwise.

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

## Security boundary

This is an engineering MVP, not a guarantee of anonymity or production readiness. Do not expose the dashboard, router console, SAM, I2CP, proxy ports, API token, private keys or destination keys to untrusted networks. The dashboard intentionally performs no remote-control discovery and telemetry is disabled.

## Planned next stage

- Tauri system-tray shell.
- Windows Service and systemd installers.
- Signed release artifacts and update verification.
- OS keychain-backed token storage.
- Tunnel profile management and destination-key backup controls.
- Integration and fault-injection tests using a mock router.
