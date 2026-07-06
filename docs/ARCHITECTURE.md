# Architecture

I2P Connect is currently `FOUNDATION_ONLY`. This document describes the intended architecture and the boundaries future runtime code must follow.

## Release Modes

`FOUNDATION_ONLY`

- Documentation, doctrine, validation scripts, and roadmap exist.
- No runtime communication code exists.
- No I2P router integration exists.
- No identity, contact, message, or media storage exists.

`LAB_SIMULATION`

- Demo or prototype behavior may exist.
- Lab behavior must be clearly labeled in UI, docs, logs, and tests.
- Lab behavior must not be promoted to release claims.
- Lab storage must avoid real private keys, private destinations, private messages, and contact graphs.

`RELEASE_REAL`

- User-facing behavior is backed by implementation and tests.
- Runtime status is measured, not assumed.
- Security-sensitive behavior has docs, tests, validation, and rollback notes.
- Release claims are listed in `docs/CLAIMS_REGISTER.md`.

## Planned Layers

1. Local application shell
   - Beginner-first interface.
   - Local settings and local status.
   - No cloud dependency for core communication.

2. I2P readiness and transport adapter
   - Detect local router reachability.
   - Prefer localhost-bound router APIs.
   - Never expose router consoles or control ports publicly.
   - Report measured states such as "router reachable" or "SAM session ready".

3. Local identity and contact model
   - Store user-controlled identity material locally.
   - Support verified invites after implementation.
   - Keep contact graphs out of cloud services by default.

4. App-layer message envelope
   - Future cryptographic envelope for messages and audio notes.
   - No full E2EE claim until cryptographic design, implementation, tests, and documentation exist.
   - Logs must never include plaintext messages, keys, private destinations, or sensitive headers.

5. Local persistence
   - Persist only what the user needs for operation, recovery, and export.
   - Use non-destructive migrations.
   - Prefer explicit export/import over background sync.

6. Diagnostics and onboarding
   - Teach measured local state.
   - Separate setup issues from privacy claims.
   - Use sanitized logs and safe status messages.

## External Service Policy

External services may be used only for non-sensitive project operations such as public documentation, issue tracking, CI, release artifacts, or opt-in sanitized metrics in a future version.

External services must not store:

- private I2P keys
- private destinations
- private messages
- contact graphs
- raw router logs
- deanonymizing metadata
- router credentials
- signing keys
- service role keys

## Runtime Code Gate

Before runtime code is added, the repository must keep passing:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

After runtime code is introduced, validation must expand to include build, lint, tests, security checks, and migration checks for the selected stack.
