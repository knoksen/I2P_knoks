# I2P Knoks Browser

I2P Knoks Browser is an Android Jetpack Compose app for honest, measurable I2P setup, diagnostics, and proxy-backed page inspection.

Current release baseline: `v0.3.0-real-alpha`.

## Real In This Alpha

- `RELEASE_REAL` mode hides lab-only VPN/VPS, chat, and peer-discovery modules from the main navigation.
- Configurable I2P endpoint setup for an external I2P or i2pd router.
- SAM bridge probing through the selected endpoint.
- HTTP proxy fetches for `.i2p` URLs through the selected endpoint.
- Local service diagnostics for SAM, HTTP proxy, and router console ports.
- Router console launch using the configured endpoint.
- Non-destructive Room migration from database v4 to v5.

## Still Lab Or Simulation

- Full WebView browsing is not implemented.
- OS-level VPN tunneling is not implemented.
- Secure chat is not audited cryptography and remains lab/demo functionality.
- Peer discovery is generated lab data.
- Embedded I2P router mode is not implemented.
- Preview page rendering is local Compose content unless the UI explicitly shows a real HTTP proxy response.

## Security Boundaries

- This app does not provide OS-level VPN tunneling.
- This app does not provide audited encrypted chat.
- This app does not provide full browser isolation.
- I2P traffic requires a running external I2P or i2pd router.
- `RELEASE_REAL` means real diagnostics, SAM probing, and HTTP proxy interactions. It does not mean anonymity by itself.

More detail: [Security Boundaries](docs/SECURITY_BOUNDARIES.md).

## Real Alpha Testing

Use the reproducible test plan before cutting or reviewing alpha releases:

- [Real Alpha Test Plan](docs/REAL_ALPHA_TEST_PLAN.md)
- [Build Toolchain](docs/BUILD_TOOLCHAIN.md)
- Local verification script: `.\scripts\local-release-verify.ps1`

## Endpoint Defaults

Default local endpoint:

| Service | Address |
| :--- | :--- |
| SAM Bridge | `127.0.0.1:7656` |
| HTTP Proxy | `127.0.0.1:4444` |
| Router Console | `127.0.0.1:7657` |

Desktop/LAN router mode requires the user to enter the real host. The app should not invent placeholder IP addresses for saved endpoint settings.

## Required Android Permissions

- `INTERNET`: required to connect to the configured local or LAN I2P router services.

No VPN service permission or background routing service is expected for the current real alpha.

## Build

Prerequisites:

- JDK 21 or newer
- Android SDK for the configured `compileSdk`
- Included Gradle wrapper

Recommended Windows JDK install:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Verify locally:

```powershell
.\scripts\local-release-verify.ps1
```

Run the release-facing claim check:

```powershell
.\scripts\check-release-claims.ps1
```

## Release Notes Template

Real:

- Endpoint setup
- Diagnostics
- SAM bridge detection/session work
- HTTP proxy `.i2p` page inspection

In active development:

- SAM lifecycle hardening with explicit HELLO, destination generation, STREAM session creation, close/reconnect, and failure states.

Not real yet:

- Full WebView browsing
- VPN/VPS
- Secure chat
- Peer discovery
- Embedded router
