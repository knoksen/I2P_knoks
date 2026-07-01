# Architecture

## Purpose

I2P Knoks Browser is an Android Jetpack Compose app for honest, measurable I2P setup, diagnostics, SAM session probing, and proxy-backed `.i2p` page inspection.

The core architecture rule is simple: anything shown as real, active, connected, or ready must come from measured runtime behavior or persisted user configuration. Anything simulated belongs in `LAB_SIMULATION` or must be clearly labeled as preview/demo content.

## App Modes

App mode is represented by `AppExperienceMode`:

- `RELEASE_REAL`: shows the real-alpha surface for endpoint setup, diagnostics, SAM session state, router console launch, and proxy-backed page inspection.
- `LAB_SIMULATION`: allows lab-only previews and generated demo surfaces.

`MainActivity` filters the top-level navigation by mode. Lab-only VPN/VPS, chat, and peer discovery modules are hidden from the main navigation in `RELEASE_REAL`.

## UI And State Flow

The UI is implemented with Jetpack Compose:

- `MainActivity` owns navigation shell and mode-aware tab visibility.
- `I2PScreens.kt` contains the Compose screens and cards.
- `I2PViewModel` coordinates repository state, endpoint validation, diagnostics, SAM lifecycle, and browser/page-inspector actions.

The ViewModel exposes measured state as flows or mutable state holders, including:

- selected endpoint config
- endpoint validation result
- diagnostics result and last diagnostics timestamp
- SAM session status
- page-inspector result metadata
- real alpha status

UI actions should call ViewModel methods instead of constructing network clients directly.

## Endpoint Config

`I2pEndpointConfig` is the source of truth for the selected I2P router endpoint:

- host
- SAM port
- HTTP proxy port
- router console port
- endpoint label

The default endpoint is `Local Android Router` at:

- SAM bridge: `127.0.0.1:7656`
- HTTP proxy: `127.0.0.1:4444`
- router console: `127.0.0.1:7657`

Endpoint config is persisted through Room as `AppSettingsEntity` and mapped in `I2PRepository`.

Endpoint validation must reject blank hosts, hosts with spaces, invalid IPv4 addresses, and ports outside `1..65535`. Invalid user input must not be silently replaced with defaults.

## Diagnostics

`I2pDiagnosticsClient` probes local or LAN services using the selected endpoint:

- SAM bridge
- HTTP proxy
- router console

The diagnostics layer reports reachability and maps port states into a user-facing summary such as ready, router unavailable, SAM disabled, HTTP proxy disabled, or partial readiness.

Diagnostics show service reachability only. They do not prove browsing anonymity, router health beyond the probed ports, or end-to-end page fetch success.

## SAM Bridge

SAM protocol handling is split into two layers:

- `SamBridgeClient`: low-level socket/protocol operations.
- `SamSessionManager`: long-lived session state, connect/disconnect lifecycle, timeout handling, and name lookup coordination.

SAM lifecycle states distinguish:

- disconnected
- connecting
- HELLO completed
- session created
- ready
- failed
- closed

Private SAM destination material must not be exposed through public UI state or logs. Public status may show public destination material and whether private material exists, but should not print the private destination value.

## HTTP Proxy Page Inspector

`I2pHttpClient` performs `.i2p` URL inspection through the configured HTTP proxy. It is not a full browser.

The inspector captures:

- fetch mode
- normalized URL and final URL
- HTTP status
- title
- content type
- content length when known
- response headers
- redirect location
- elapsed time
- fetch timestamp
- safe body preview or error details

Redirects are captured and shown, not automatically followed. HTML is converted to a limited safe text preview by `SafePreviewSanitizer`; scripts and styles are removed, preview length is capped, and remote HTML/JavaScript is never executed.

The real alpha intentionally has no WebView execution.

## Room Persistence

Room persistence is defined in:

- `AppDatabase`
- `Entities.kt`
- `Daos.kt`
- `I2PRepository`

The database stores app data such as identities, bookmarks, messages, logs, contacts, and app settings. Schema migrations must preserve existing local data when possible. Destructive migrations are not acceptable for real-alpha release paths that add settings or metadata tables.

## Security Boundaries

Security and privacy boundaries are documented in `docs/SECURITY_BOUNDARIES.md` and mirrored in the app UI.

Current real-alpha limits:

- no OS-level VPN tunneling
- no audited encrypted chat
- no full browser isolation
- no WebView/full browser execution
- no embedded I2P router
- no Tor integration
- external Java I2P or i2pd router required for real I2P behavior

`RELEASE_REAL` means measured diagnostics, SAM probing/session state, endpoint configuration, router console links, and HTTP proxy page inspection. It does not mean anonymity by itself.

## Release Versus LAB Modules

Release-facing text and UI must avoid unsupported claims. Do not describe lab/demo surfaces as active protection, secure routing, anonymous browsing, encrypted messaging, or real peer discovery unless those claims are backed by implemented and tested runtime behavior.

The release claim check script should remain part of local and CI release verification:

```powershell
.\scripts\check-release-claims.ps1
```

## Build And Release

Local release verification is centralized in:

```powershell
.\scripts\local-release-verify.ps1
```

The Android GitHub Actions workflow builds the debug APK and uploads it as:

- `i2p-knoks-debug-apk`

Release process details live in `docs/RELEASE_PROCESS.md`.

## Future Multi-Engine Design

The current real release path is external-router mode using Java I2P or i2pd plus local/LAN SAM and HTTP proxy ports.

Future multi-engine work should introduce a small engine boundary before adding new backends. A future `NetworkEngine` abstraction should expose diagnostics, connection, fetch, and capability reporting without making unsupported claims.

Candidate future engines:

- Java I2P external router
- i2pd external router
- local proxy-only mode
- lab simulation mode

Do not add Tor, VPN, or embedded-router options as real engines until they are implemented, tested, documented, and reflected accurately in the security boundaries.

