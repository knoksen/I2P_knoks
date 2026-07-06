# Codex Task 05 - Add Audio Message Prototype

## Prompt

Add an audio note prototype for I2P Connect as encrypted message attachments.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/MVP_SCOPE.md`
- `product/i2p-connect/SECURITY_MODEL.md`
- `product/i2p-connect/RESPONSIBLE_USE.md`
- `docs/ANDROID_PERMISSIONS.md`
- `app/src/main/AndroidManifest.xml`

## Requirements

- Add audio only behind an explicit feature gate or lab flag until release-ready.
- Request `RECORD_AUDIO` only when recording is implemented.
- Update Android permission docs if the manifest changes.
- Add short duration and size caps.
- Store encrypted attachment data.
- Playback only after decrypt.
- Never record in the background.
- Never auto-send audio without an explicit send action.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- Permission denial is handled cleanly.
- Recording state is visible.
- Duration and size are shown.
- Audio attachment follows the same delivery states as text messages.
- Delete removes local audio data.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

If Android permissions changed, also verify the release candidate permission checklist.
