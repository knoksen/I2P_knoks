# I2P Connect

I2P Connect is a proposed product module for a private, I2P-native communication app inside the I2P Knoks roadmap.

The product is inspired by the usability lessons of cloud phone systems: clear identity, simple contact setup, routing-aware status, shared team spaces, and low-friction voice workflows. It adapts those ideas to I2P instead of copying internet telephony expectations.

The first useful I2P Connect release path is planned as asynchronous communication, not live video, PSTN calling, or a Zoom replacement. The MVP should make private text messages, encrypted audio notes, contact identity, router readiness checks, and beginner onboarding feel understandable and measurable after implementation and tests exist.

## Package Contents

- [PRODUCT_SPEC.md](PRODUCT_SPEC.md) - product goals, users, workflows, and non-goals.
- [ARCHITECTURE.md](ARCHITECTURE.md) - practical app architecture and module boundaries.
- [MVP_SCOPE.md](MVP_SCOPE.md) - phase 1 scope and explicit exclusions.
- [SECURITY_MODEL.md](SECURITY_MODEL.md) - trust boundaries, data handling, and crypto requirements.
- [RESPONSIBLE_USE.md](RESPONSIBLE_USE.md) - acceptable-use framing and safety UX.
- [ROADMAP.md](ROADMAP.md) - staged path from private messaging to education kit.
- [codex/](codex/) - implementation prompts for Codex tasks.

## North Star

Zoom made digital telephony easy for business users.

I2P Connect should make private, human communication easier for open communities without pretending I2P is optimized for high-bandwidth real-time media.

## Recommended Sequence

1. Private messages and encrypted audio messages.
2. PBX-light concepts such as team rooms, routing labels, and shared inboxes.
3. Push-to-talk voice after reliability and latency tests.
4. Experimental video only after measured network results support an honest preview.
5. Community adoption and education kit.

## Current Repository Fit

The current app already has real-alpha patterns that this module should reuse:

- `RELEASE_REAL` must only show measured runtime behavior.
- Lab messaging and contact features are not audited and should not be presented as production communication.
- I2P readiness is measured through endpoint configuration, diagnostics, SAM probing, and HTTP proxy checks.
- Private SAM destination material must not be exposed in UI state or logs.

## Doctrine Lock

All I2P Connect implementation tasks should read these repo governance files before changing code:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`

The module should follow the doctrine checklist in [MVP_SCOPE.md](MVP_SCOPE.md) before any release-facing work is considered complete.
