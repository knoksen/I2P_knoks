# AGENTS.md

This file is the operating contract for Codex and other repository agents working on I2P Connect.

The project rule is strict: user-facing behavior must be grounded in measured runtime state, persisted user configuration, tested implementation, or clearly labeled lab/demo behavior. Product ambition is welcome, but release-facing claims must stay honest.

## Required Reading

Before making product, app, security, onboarding, or release-facing changes, read:

1. `README.md`
2. `DIGITAL_AUTONOMY_DOCTRINE.md`
3. `product/03_UX_ONBOARDING_SPEC.md`
4. `docs/ARCHITECTURE.md`
5. `docs/SECURITY_BOUNDARIES.md`
6. `docs/ANDROID_PERMISSIONS.md`
7. `docs/RESPONSIBLE_USE.md`
8. `docs/ROADMAP.md`
9. `docs/CLAIMS_REGISTER.md`
10. Any module docs relevant to the task.

If one of these files is missing, say so in the final summary and continue only from the available repository context. Do not invent missing governance.

## Doctrine Lock

Every implementation must preserve:

- measured status over hopeful labels
- local user control over identity, contacts, logs, and exports
- beginner onboarding that explains what is real, what is lab, and what action comes next
- minimal Android permission scope if Android code is added
- no private keys, private destinations, plaintext messages, credentials, or sensitive headers in logs
- non-destructive persistence migrations for release paths
- clear separation between `RELEASE_REAL`, `LAB_SIMULATION`, and `FOUNDATION_ONLY`
- responsible-use copy before risky actions
- validation before completion

## Release-Facing Claim Rules

Do not describe a feature as active, connected, private, encrypted, anonymous, routed, protected, production-ready, release-ready, SASE, zero-trust, or full E2EE unless the repository contains implementation and tests that support that claim.

Use precise language:

- "router reachable"
- "SAM session ready"
- "message queued"
- "payload encrypted by the app-layer envelope"
- "local preview"
- "not externally audited"
- "experimental"

Avoid broad safety promises, mainstream video-conferencing expectations, phone-network replacement language, emergency-use language, or enterprise compliance language unless explicitly implemented, tested, and documented.

## I2P Connect Direction

I2P Connect should prioritize:

1. Private messages and encrypted audio notes after the cryptographic design and implementation are present.
2. Contact identity and verified invites.
3. I2P readiness checks.
4. Beginner onboarding missions.
5. PBX-light and team routing only after the messaging foundation works.
6. Push-to-talk and video only as measured experiments.

Do not claim Zoom-like real-time video quality.

## Security and OpSec Rules

Treat I2P identity material, tunnel keys, router credentials, private destinations, API keys, service role keys, signing keys, and database credentials as secrets.

Never print, commit, log, or hardcode secrets.

Use `.env.example` for variable names only if runtime configuration is introduced later.

Prefer:

- localhost-bound admin ports
- least-privilege containers if Docker is introduced
- non-root containers where possible
- explicit network isolation
- no public exposure of router consoles
- safe defaults
- opt-in telemetry only
- sanitized logs
- release-claim validation
- tests before claims

Warn clearly if a requested change could cause:

- IP leaks
- privacy leaks
- deanonymization risk
- public exposure of admin services
- metadata leakage
- unsafe storage of identifiers
- unsafe use of Supabase, external APIs, or cloud services

## Development Expectations

- Prefer the repository's existing patterns once runtime code exists.
- Keep changes scoped to the task.
- Preserve unrelated local changes.
- Add or update tests when changing behavior.
- Update docs when permissions, security boundaries, or release-facing claims change.
- Keep lab/demo flows labeled and hidden from `RELEASE_REAL` unless backed by real implementation.
- Do not add cloud storage for private I2P keys, private destinations, private messages, contact graphs, raw router logs, or deanonymizing metadata.

## Validation

At minimum, run:

```powershell
.\scripts\check-release-claims.ps1
```

For release-facing, app, security, onboarding, or product-pack changes, run:

```powershell
.\scripts\local-release-verify.ps1
```

If validation cannot run, report the exact blocker and the last successful narrower command.
