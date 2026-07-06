# Codex Task 01 - Build Local I2P Status Service

## Prompt

Implement an I2P Connect readiness service that composes the existing endpoint config, diagnostics, SAM session state, and local identity availability into one communication status model.

Read:

- `AGENTS.md`
- `DIGITAL_AUTONOMY_DOCTRINE.md`
- `product/03_UX_ONBOARDING_SPEC.md`
- `product/i2p-connect/ARCHITECTURE.md`
- `product/i2p-connect/MVP_SCOPE.md`
- `app/src/main/java/no/knoksen/i2pbrowser/i2p/I2pDiagnosticsClient.kt`
- `app/src/main/java/no/knoksen/i2pbrowser/i2p/RealAlphaStatus.kt`
- `app/src/main/java/no/knoksen/i2pbrowser/i2p/SamSessionManager.kt`
- `app/src/main/java/no/knoksen/i2pbrowser/ui/I2PViewModel.kt`

## Requirements

- Add a small status model under a Connect namespace.
- Map router readiness, SAM readiness, identity readiness, and message-send eligibility.
- Include a recommended next action for each blocked state.
- Keep UI wording claim-safe.
- Do not mark messaging ready from simulated state.
- Apply the doctrine checklist from `product/i2p-connect/MVP_SCOPE.md`.

## Acceptance Criteria

- Unit tests cover all readiness combinations.
- Status does not expose private destination material.
- Release mode cannot show communication readiness without measured diagnostics and identity state.

## Validation

Run:

```powershell
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```
