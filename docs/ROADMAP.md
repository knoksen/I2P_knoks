# Roadmap

This roadmap tracks real-alpha hardening work for I2P Knoks Browser.

The project rule remains strict: release-facing behavior and wording must be backed by measured runtime state, verified responses, or clearly documented limits. Lab/simulation features must not be presented as real privacy, anonymity, VPN, Tor, protected transport, or cryptographic guarantees.

## Done

- [x] M9.3 - Release-facing claim CI gate
  - Landed on `main` via PR #17.
  - Added `Release Claim Check` GitHub Actions workflow.
  - Added least-privilege GitHub Actions permissions.
  - Added `ready_for_review` pull request trigger.
  - Under Ubuntu + PowerShell, a no-match `rg` result can leave `$LASTEXITCODE = 1` even when the check should pass.
  - `scripts/check-release-claims.ps1` now resets `$global:LASTEXITCODE = 0` after a clean pass.
  - Android CI must use JDK 21 to match the repository release verification baseline.
  - Release claim rules were not weakened while fixing CI.
  - Remote PR #17 checks were green before merge:
    - `Release Claim Check`: success
    - `Android`: success
  - Follow-up issues:
    - #18 M9.4 release candidate readiness checklist
    - #19 Android manifest and permissions documentation
    - #20 RELEASE_REAL wording audit
    - #21 APK checksum and artifact verification
    - #22 CI lessons from M9.3
- [x] M9.4 groundwork
  - #19 documented the Android permission boundary.
  - #20 audited `RELEASE_REAL` UI wording.
  - #21 added APK checksum and artifact verification.
  - #18 adds the release candidate readiness checklist and connects the existing manual readiness workflow to it.
  - #22 documents CI lessons from M9.3, including PowerShell/ripgrep exit handling, JDK baseline alignment, Windows Gradle lock cleanup, and release-claim gate discipline.

## Next

### M9.4 - Release Candidate Readiness Gate

Objective: create a release candidate readiness gate that verifies build artifacts, release notes, Android permissions, `RELEASE_REAL` boundaries, APK checksums, and claim-safe documentation before tagging or publishing an alpha release.

First tasks:

- Use `docs/RELEASE_CANDIDATE_CHECKLIST.md` as the manual RC review gate.
- Use the existing manual `Release Candidate Readiness Checklist` workflow as a summary and confirmation gate.
- Keep APK SHA-256 checksum generation in the Android and draft-release workflows.
- Verify Android manifest permissions against docs.
- Verify release notes do not contain blocked claims.
- Verify `RELEASE_REAL` hides lab/simulation tabs.
- Add CI summary with pass/fail table.

Recommended issue order:

1. #19 - Manifest/permissions first.
2. #20 - `RELEASE_REAL` wording audit.
3. #21 - APK checksum and artifact verification.
4. #18 - M9.4 release candidate checklist.
5. #22 - CI lessons from M9.3.

## Manual Repository Settings

Branch protection / ruleset configuration is required to make M9.3 a merge-blocking policy instead of only an operational workflow.

Recommended `main` protection:

- Require a pull request before merging.
- Require status checks to pass before merging.
- Require branches to be up to date before merging.
- Require conversation resolution before merging.
- Require `Release Claim Check / Check release-facing claims`.
- Require `Android / build`.
- Do not allow force pushes.
- Do not allow deletions.
- Enable no-bypass behavior if admins should also follow the rule.
