# Codex Task 03 - Build Contact Invite Flow

## Prompt

Build the contact invite create/import/verify flow for I2P Connect.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/PRODUCT_SPEC.md`
- `product/i2p-connect/SECURITY_MODEL.md`
- `product/i2p-connect/RESPONSIBLE_USE.md`
- `product/i2p-connect/MVP_SCOPE.md`
- existing contact and trusted key code in `app/src/main/java/no/knoksen/i2pbrowser/data`

## Requirements

- Define a versioned text invite schema.
- Include display name, public destination, public app key, fingerprint, and timestamp.
- Implement strict parser validation with useful errors.
- Store imported contacts as unverified by default.
- Add explicit verify/unverify states.
- Show key-change warnings when an existing contact's public key changes.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- Valid invite imports as unverified.
- Malformed, oversized, or missing-field invites are rejected.
- Verification state is visible and persisted.
- Invite strings and full keys are not written to logs.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```
