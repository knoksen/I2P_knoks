# Codex Task 06 - Add Onboarding Missions

## Prompt

Add beginner onboarding missions for I2P Connect.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/PRODUCT_SPEC.md`
- `product/i2p-connect/MVP_SCOPE.md`
- `product/i2p-connect/RESPONSIBLE_USE.md`
- existing setup/status UI in `app/src/main/java/no/knoksen/i2pbrowser/ui/I2PScreens.kt`

## Requirements

- Create a mission checklist for:
  - run I2P status check
  - create local identity
  - import or copy invite
  - verify contact fingerprint
  - send first private text message
  - send first audio note when enabled
  - review safety boundaries
- Completion should come from measured state where possible.
- Keep wording beginner-friendly and claim-safe.
- Do not add large explanatory pages before the usable workflow.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- User can see next recommended mission.
- Missions deep-link or navigate to the relevant flow.
- Completed missions persist locally.
- Safety review is visible before first message/audio action.
- Release mode does not show missions for disabled lab-only features.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```
