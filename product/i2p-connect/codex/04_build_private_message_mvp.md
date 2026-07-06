# Codex Task 04 - Build Private Message MVP

## Prompt

Build the first I2P Connect private message MVP using explicit encrypted envelopes and queue-aware delivery state.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/ARCHITECTURE.md`
- `product/i2p-connect/MVP_SCOPE.md`
- `product/i2p-connect/SECURITY_MODEL.md`
- `app/src/main/java/no/knoksen/i2pbrowser/i2p/SamSessionManager.kt`
- existing demo message code in `app/src/main/java/no/knoksen/i2pbrowser/data/I2PRepository.kt`

## Requirements

- Do not reuse Base64 demo encoding as encryption.
- Use a reviewed Android-compatible crypto library or create an interface with a clearly marked test-only implementation until the library is selected.
- Add message envelope models and delivery states.
- Queue outbound messages when readiness is incomplete.
- Send only to verified contacts.
- Never log plaintext or decrypted bodies.
- Make direct-online limitations clear in UI copy.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- User can compose a message to a verified contact.
- Outbound state moves through drafted, queued, sending, sent, or failed.
- Inbound envelopes can be stored and decrypted in tests.
- Duplicate envelope ids do not create duplicate visible messages.
- Tests cover crypto interface, state transitions, and log sanitizer behavior.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```
