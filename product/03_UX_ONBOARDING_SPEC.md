# UX Onboarding Spec

I2P Connect onboarding must help beginners understand private communication over I2P without fear, hype, or unsupported guarantees.

## Experience Goals

Onboarding should make users feel:

- oriented
- in control
- aware of limits
- able to take the next safe step
- invited into digital autonomy without panic or jargon

## First-Run Direction

The first runtime onboarding should start with measured truth:

- what I2P Connect can do in this version
- what is local
- what is experimental
- what is not implemented
- what action comes next

Current repository state should be described as foundation-only until runtime code exists.

## Beginner Missions

Future onboarding may use missions:

1. Understand the mode
   - Shows `FOUNDATION_ONLY`, `LAB_SIMULATION`, or `RELEASE_REAL`.
   - Explains what claims are supported in the current version.

2. Check I2P readiness
   - Measures local router reachability.
   - Explains how to start or connect to a local router.
   - Avoids claiming traffic is private merely because a setup step passed.

3. Create local identity
   - Explains where identity material is stored.
   - Explains backup and loss risks.
   - Never uploads identity material by default.

4. Verify a contact
   - Explains invites, fingerprints, or verification words after implemented.
   - Warns that unsafe verification can weaken privacy.

5. Send a first message or audio note
   - Enabled only after transport, identity, storage, and encryption requirements exist.
   - Shows measured queue and delivery states.
   - Avoids broad anonymity or security promises.

## Copy Rules

Use:

- "router reachable"
- "local preview"
- "not externally audited"
- "experimental"
- "message queued"
- "payload encrypted by the app-layer envelope" only after implementation and tests

Do not use:

- "guaranteed anonymity"
- "untraceable"
- "full E2EE" before implementation and tests
- "zero-trust" before implementation and tests
- "SASE" before implementation and tests
- "real-time video" as a release promise
- "Zoom-like" as a quality promise

## Risk Moments

Future UI must show responsible-use or safety copy before:

- creating or exporting identity material
- importing identities
- sharing invites
- enabling logs
- exporting diagnostics
- adding contacts
- enabling audio capture
- enabling any cloud-connected feature

## Accessibility and Tone

The experience should be calm, plain-language, and beginner-friendly. It should avoid fear marketing, criminal symbolism, and hacker-only aesthetics.
