# I2P Connect

I2P Connect is a planned secure, beginner-friendly, local-first communication platform for digital autonomy on I2P.

This repository is currently **foundation-only**. No runtime communication code is present yet. The first version establishes doctrine, security boundaries, onboarding direction, responsible-use language, validation scripts, and a safe roadmap before implementation begins.

## Current State

Supported claims in this repository today:

- Project doctrine and contributor operating rules are documented.
- Security boundaries and data handling restrictions are documented.
- Beginner onboarding direction is documented.
- Responsible-use expectations are documented.
- Local validation scripts exist for release-facing claim checks.
- No app runtime, message transport, identity store, or I2P router integration has been added yet.

Unsupported claims today:

- I2P Connect is not a Zoom clone.
- I2P Connect does not promise real-time video.
- I2P Connect does not promise guaranteed anonymity.
- I2P Connect does not claim full E2EE, SASE, or zero-trust unless those properties are implemented, tested, documented, and release-gated in a future version.
- I2P Connect does not store private I2P keys, private destinations, private messages, contact graphs, raw router logs, or deanonymizing metadata in cloud services.

## Product Direction

The first runtime versions should prioritize:

- Private messages and encrypted audio notes after app-layer encryption is implemented and tested.
- Contact identity and verified invites.
- I2P readiness checks that explain measured local router state.
- Beginner onboarding missions that make privacy tools understandable without fear marketing.
- PBX-light and team routing only after the messaging foundation is real.
- Push-to-talk and video only as clearly labeled experiments, with no promise of mainstream real-time video quality.

## Safety Defaults

I2P Connect must prefer:

- Local-first storage for identities, contacts, messages, router diagnostics, and exports.
- Least-privilege access to router APIs and local services.
- Localhost-bound admin interfaces.
- Sanitized logs that avoid private destinations, credentials, message bodies, sensitive headers, and raw router logs.
- Clear separation between `RELEASE_REAL`, `LAB_SIMULATION`, and `FOUNDATION_ONLY` behavior.
- Honest user-facing copy that says what is measured, what is experimental, and what is not implemented yet.

## Repository Map

- `AGENTS.md`: operating contract for Codex and other coding agents.
- `DIGITAL_AUTONOMY_DOCTRINE.md`: project doctrine and tone.
- `docs/ARCHITECTURE.md`: planned architecture and release modes.
- `docs/SECURITY_BOUNDARIES.md`: threat model, data classes, and prohibited storage.
- `docs/ANDROID_PERMISSIONS.md`: future Android permission posture.
- `docs/RESPONSIBLE_USE.md`: responsible-use and safety expectations.
- `docs/ROADMAP.md`: safe implementation sequence.
- `docs/CLAIMS_REGISTER.md`: supported and unsupported claim register.
- `docs/VALIDATION.md`: validation script reference.
- `product/03_UX_ONBOARDING_SPEC.md`: onboarding and beginner experience direction.
- `scripts/check-release-claims.ps1`: release-facing claim guard.
- `scripts/local-release-verify.ps1`: local foundation verification entrypoint.

## Validation

Run the claim guard:

```powershell
.\scripts\check-release-claims.ps1
```

Run the local foundation verification:

```powershell
.\scripts\local-release-verify.ps1
```

The foundation verification intentionally checks that no runtime application source has been added in this first phase.

## Contributing

Before changing product, app, onboarding, security, or release-facing behavior, read `AGENTS.md` and the required project docs listed there.

Keep changes small, testable, and honest. Release-facing language must be grounded in measured runtime state, persisted user configuration, tested implementation, or clearly labeled lab/demo behavior.
