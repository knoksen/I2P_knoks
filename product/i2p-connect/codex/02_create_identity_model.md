# Codex Task 02 - Create Identity Model

## Prompt

Create the I2P Connect identity model and persistence layer.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/SECURITY_MODEL.md`
- `product/i2p-connect/MVP_SCOPE.md`
- `app/src/main/java/no/knoksen/i2pbrowser/data/Entities.kt`
- `app/src/main/java/no/knoksen/i2pbrowser/data/AppDatabase.kt`
- `app/src/test/java/no/knoksen/i2pbrowser/AppDatabaseMigrationTest.kt`
- `app/src/test/java/no/knoksen/i2pbrowser/LogSanitizerTest.kt`

## Requirements

- Add explicit `connect_identities` storage instead of silently reusing the demo `identities` table.
- Store public metadata in Room.
- Store private material only through a protected reference or clearly marked placeholder until secure storage is implemented.
- Generate and display a short fingerprint from public material.
- Add non-destructive migration tests.
- Extend log sanitizer tests for identity private material.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- A local identity can be created and listed.
- Private material is never logged.
- Migration preserves existing data.
- UI/state models expose only public destination and fingerprint.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```
