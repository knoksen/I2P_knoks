# Security Boundaries

I2P Knoks Browser uses strict wording for real alpha behavior: anything shown as real, active, or connected must be backed by measured runtime state or a verified response.

## RELEASE_REAL

`RELEASE_REAL` means the app may show real local I2P diagnostics, SAM probing/session state, endpoint configuration, router console links, and HTTP proxy page inspection when those actions actually run.

It does not mean the app provides anonymity by itself.

## LAB_SIMULATION

`LAB_SIMULATION` is for local UI previews, generated telemetry, demo messaging, and experimental workflows. Lab features must be labeled as simulation/demo/preview and must not be described as active protection.

## Not Provided In This Alpha

- No OS-level VPN tunnel.
- No audited encrypted chat.
- No full browser isolation.
- No embedded I2P router.
- No Tor integration.
- No background routing service.

## Required External Component

Real I2P behavior requires a running I2P or i2pd router reachable from the Android device or emulator.

Android permission scope is documented separately in [Android Permissions](ANDROID_PERMISSIONS.md).

Default local services:

| Service | Default |
| :--- | :--- |
| SAM Bridge | `127.0.0.1:7656` |
| HTTP Proxy | `127.0.0.1:4444` |
| Router Console | `127.0.0.1:7657` |

## Android Permissions

The real alpha requests `INTERNET` so it can connect to the configured I2P or i2pd router services. It does not request VPN, background routing, location, contacts, SMS, or network-state permissions.

## Logging Boundary

Logs should help troubleshoot local service state without storing sensitive material. Do not log private destination keys, credentials, API keys, full message bodies, or sensitive headers.

Application log messages are sanitized before they are stored in Room. The sanitizer redacts obvious private SAM destination material, credentials, API keys, sensitive headers, and message body fields, and caps very long log messages.
