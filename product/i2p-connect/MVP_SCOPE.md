# MVP Scope

## MVP Name

I2P Connect Phase 1: private messages and audio notes.

## In Scope

### Status

- Reuse endpoint configuration.
- Show router/SAM readiness before communication.
- Show whether a local communication identity exists.
- Queue send attempts when readiness is incomplete.

### Identity

- Create one local I2P Connect identity.
- Store public identity metadata in Room.
- Store private material behind a protected reference.
- Show a short fingerprint.
- Copy a public contact invite.

### Contacts

- Import a text invite.
- Validate required fields.
- Show fingerprint before trust.
- Store unverified and verified contacts.
- Allow deleting contacts.

### Private Text Messaging

- Compose text message to a verified contact.
- Encrypt payload with an explicit app-layer envelope.
- Store outbound message state.
- Attempt send through I2P transport only when status allows.
- Receive and decrypt message envelopes for the active identity.
- Show delivery and decrypt states without pretending reliability.

### Audio Notes

- Prototype short voice notes as message attachments.
- Cap recording duration.
- Encrypt attachment payload.
- Show duration, size, and delivery state.
- Disable or hide audio in release mode until microphone permission, storage, encryption, and playback are tested.

### Beginner Onboarding

- Add mission checklist for setup.
- Link each mission to the relevant screen/action.
- Mark completion only from measured app state or explicit user confirmation for safety reading.

## Out Of Scope

- Out of scope: real-time video.
- Live conferencing.
- PSTN/SIP calling.
- Contact discovery across the network.
- Phone contacts import.
- Push notifications.
- Group administration.
- Team rooms.
- PBX routing.
- Moderation infrastructure.
- Enterprise compliance.
- High-risk user guarantees.

## MVP Release Gates

- Existing local release verification passes.
- Release claim check passes.
- New Room migrations are non-destructive.
- Log sanitizer tests cover message and identity secrets.
- UI copy does not call lab/demo flows production.
- Android permission docs are updated before adding microphone permission.
- The app says when both parties must be online.

## Doctrine Checklist

Before any I2P Connect implementation task is marked complete:

- [ ] `AGENTS.md` was read for repo operating rules.
- [ ] `DIGITAL_AUTONOMY_DOCTRINE.md` was read for product and safety commitments.
- [ ] `product/03_UX_ONBOARDING_SPEC.md` was read for beginner onboarding requirements.
- [ ] User-facing state is measured, persisted, or clearly labeled as lab/demo.
- [ ] `RELEASE_REAL` does not show unsupported communication features.
- [ ] Private keys, private destinations, plaintext messages, credentials, and sensitive headers are absent from logs.
- [ ] New permissions are avoided unless the implemented feature requires them.
- [ ] Risky actions have concise responsible-use copy near the action.
- [ ] New persistence changes use non-destructive migrations and tests.
- [ ] `.\scripts\check-release-claims.ps1` and `.\scripts\local-release-verify.ps1` were run, or any blocker is reported with exact failure output.

## Acceptance Criteria

The MVP can be considered ready for a private alpha when:

- A fresh install can run communication readiness checks.
- A user can create one local identity.
- A user can export and import a contact invite.
- A user can verify a contact fingerprint.
- A user can send a private text message to a reachable test peer.
- A user can receive and decrypt a private text message from a reachable test peer.
- A short audio note prototype works in a controlled test build.
- Every failure state has a recovery hint.

## Honest Product Copy

Use:

- "private message"
- "encrypted payload"
- "queued until I2P is ready"
- "short audio note"
- "not audited"
- "experimental"

Do not use:

- "video meeting"
- "phone replacement"
- "always online"
- "perfect privacy"
- "enterprise PBX"
- "emergency-ready"
