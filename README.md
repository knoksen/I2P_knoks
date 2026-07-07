# I2P Knoks Browser And I2P Connect

This repository contains an Android real-alpha app for local I2P diagnostics and `.i2p` page inspection, plus product-planning documents for the I2P Connect communication path.

The current source tree is no longer foundation-only. It includes Android runtime code, tests, validation scripts, security boundaries, release-process docs, and I2P Connect planning materials. The current alpha is not independently audited and does not currently guarantee anonymity, encrypted chat, VPN behavior, Tor routing, or browser isolation.

## Release Maturity

Current maturity: **real-alpha**.

Real-alpha means the app may show source-backed and measured behavior such as configured endpoint status, local I2P service diagnostics, SAM session state, and HTTP proxy inspection results when those checks actually run.

It does not mean the app is broadly reviewed, complete, or suitable for high-risk use. Release-facing wording must stay grounded in implementation, tests, measured runtime state, persisted local configuration, or clearly labeled lab/demo behavior.

## Currently Implemented

The following functionality is available in the Android source tree and covered by source code and/or unit tests:

- Android app shell with `RELEASE_REAL` and `LAB_SIMULATION` modes.
- `RELEASE_REAL` shows router and browser surfaces by default; lab-only communication and VPN/VPS surfaces are hidden by tests.
- Configurable I2P endpoint settings for local, emulator, or LAN I2P/i2pd services.
- Persisted endpoint configuration in Room through `app_settings`.
- Local diagnostics for configured SAM Bridge, HTTP proxy, and router console ports.
- Readiness mapping for ready, partial, offline, and unchecked local I2P service states.
- SAM client/session work for HELLO, destination generation, session creation, compatibility fallback, close, timeout handling, and `.i2p` name lookup.
- `.i2p` page inspection through the configured HTTP proxy, including status metadata and sanitized text preview.
- Non-`.i2p` and invalid URL handling that avoids sending those requests through the I2P HTTP proxy path.
- Security-boundary UI copy that states the alpha limits inside the app.
- Connect identity model, Room table, deterministic public fingerprint helper, and public-only export/import helpers.
- Connect identity import/export tests that reject private-material fields and keep cloud sync disabled by default.
- Log sanitizer tests for private SAM material, credentials, sensitive headers, message/plaintext fields, private-material references, and long log truncation.
- Android unit tests for endpoint validation/normalization, diagnostics mapping, diagnostic timeout/cancellation behavior, SAM client/session behavior, HTTP proxy result handling, safe preview sanitization, mode boundaries, identity helpers, Room schema/migration graph guards, and database migration SQL.
- Android instrumentation migration tests are available for supported Room 4->5->6 paths and require an emulator/device or the CI emulator job to execute.
- GitHub Actions workflows and local scripts for release-claim checks, Android unit tests, debug APK build, artifact upload, and checksum generation.

## Lab Or Prototype Behavior

Some source-tree surfaces are intentionally lab/demo behavior and must not be described as active protection:

- Lab VPN/VPS UI is local sample behavior, not OS-level tunneling.
- Lab chat/communications UI is demo behavior, not audited encrypted messaging.
- Legacy demo identity, contact, trusted-key, and `secure_messages` flows are not the production-bound I2P Connect communication model.
- Demo message bodies may be Base64-encoded for local UI preview; that is not app-layer cryptography.
- Simulated NetDB, route, latency, peer, and tunnel controls are lab previews unless backed by measured runtime state.
- Invitation-lab helpers are prototype utilities and do not prove contact ownership or transport safety.

## Planned I2P Connect Work

I2P Connect remains the planned communication path for private messages, verified contacts, app-layer message envelopes, and short audio notes. Those features should become release-facing only after implementation, tests, docs, permissions review, security-boundary review, and release-claim validation.

Near-term planned work is tracked in:

- [I2P Connect MVP Scope](product/i2p-connect/MVP_SCOPE.md)
- [Advanced Upgrades TODO](docs/ADVANCED_UPGRADES_TODO.md)
- [Roadmap](docs/ROADMAP.md)

## Known Limitations

- The app depends on an external I2P or i2pd router for real I2P behavior.
- The current manifest requests `android.permission.INTERNET`; it does not request VPN, background routing, storage, contacts, SMS, location, camera, or microphone permissions.
- The app does not provide anonymity by itself.
- The app does not provide an OS-level VPN tunnel.
- The app does not provide Tor routing.
- The app does not provide an embedded I2P router.
- The app does not provide audited encrypted chat.
- The app does not provide full browser isolation or JavaScript execution.
- Debug APK artifacts are for real-alpha testing unless release notes say otherwise.
- Audio notes, verified contact messaging, and app-layer encrypted message envelopes remain gated future work.

## Unsupported Claims

The older foundation-only / no runtime communication code status no longer describes the repository as a whole because Android runtime code is now present.

- I2P Connect is not a Zoom clone.
- I2P Connect does not promise guaranteed anonymity.
- I2P Connect does not claim full E2EE, SASE, or zero-trust unless those properties are implemented, tested, documented, and release-gated in a future version.
- I2P Connect does not store private I2P keys, private destinations, private messages, contact graphs, raw router logs, or deanonymizing metadata in cloud services.

## Safety Defaults

I2P Knoks Browser and I2P Connect work must prefer:

- measured status over hopeful labels
- local-first control of identities, contacts, messages, diagnostics, and exports
- least-privilege access to router APIs and local services
- localhost-safe defaults for admin and control endpoints
- clear separation between `RELEASE_REAL` and `LAB_SIMULATION`
- sanitized logs that avoid private destinations, credentials, message bodies, sensitive headers, and raw router logs
- honest user-facing copy that says what is measured, what is experimental, and what is not implemented

## Repository Map

- [AGENTS.md](AGENTS.md): operating contract for Codex and other coding agents.
- [SECURITY.md](SECURITY.md): authoritative security policy and vulnerability-reporting guidance.
- [DIGITAL_AUTONOMY_DOCTRINE.md](DIGITAL_AUTONOMY_DOCTRINE.md): project doctrine and tone.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): architecture and release-mode boundaries.
- [docs/SECURITY_BOUNDARIES.md](docs/SECURITY_BOUNDARIES.md): real-alpha security boundaries and prohibited claims.
- [docs/ANDROID_PERMISSIONS.md](docs/ANDROID_PERMISSIONS.md): Android permission boundary.
- [docs/RESPONSIBLE_USE.md](docs/RESPONSIBLE_USE.md): responsible-use expectations.
- [docs/CLAIMS_REGISTER.md](docs/CLAIMS_REGISTER.md): supported, unsupported, and gated claim register.
- [docs/CLAIM_SAFE_WRITING.md](docs/CLAIM_SAFE_WRITING.md): writing rules for claim-safe public text.
- [docs/VALIDATION.md](docs/VALIDATION.md): local validation command reference.
- [docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md](docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md): traceable real-alpha core-flow test matrix.
- [docs/ADVANCED_UPGRADES_TODO.md](docs/ADVANCED_UPGRADES_TODO.md): prioritized next-phase task list.
- [docs/ROADMAP.md](docs/ROADMAP.md): real-alpha hardening roadmap.
- [product/03_UX_ONBOARDING_SPEC.md](product/03_UX_ONBOARDING_SPEC.md): onboarding and beginner experience direction.
- [product/i2p-connect/](product/i2p-connect/): I2P Connect product planning package.
- [scripts/check-release-claims.ps1](scripts/check-release-claims.ps1): release-facing claim guard.
- [scripts/local-release-verify.ps1](scripts/local-release-verify.ps1): local release verification entrypoint.

## Validation

Run the fast claim guard for documentation, product, app, onboarding, security, or release-facing changes:

```powershell
.\scripts\check-release-claims.ps1
```

Run the local release verifier before considering alpha-impacting work complete:

```powershell
.\scripts\local-release-verify.ps1
```

The verifier checks release-facing claims, runs Android unit tests, and builds the debug APK. See [Validation](docs/VALIDATION.md) for the command reference and failure-reporting rules.

## Contributing

Before changing product, app, onboarding, security, or release-facing behavior, read `AGENTS.md` and the required project docs listed there.

Keep changes small, testable, and honest. Do not introduce unsupported privacy, anonymity, security, routing, encryption, compliance, or release-maturity claims without implementation, tests, documentation, and validation evidence.
