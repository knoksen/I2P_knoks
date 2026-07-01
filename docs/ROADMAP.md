# Roadmap

This roadmap tracks real-alpha hardening work for I2P Knoks Browser.

The project rule remains strict: release-facing behavior and wording must be backed by measured runtime state, verified responses, or clearly documented limits. Lab/simulation features must not be presented as real privacy, anonymity, VPN, Tor, protected transport, or cryptographic guarantees.

## Done

- [x] M9.3 - Release-facing claim CI gate
  - Landed on `main` via PR #17.
  - Added `Release Claim Check` GitHub Actions workflow.
  - Added least-privilege GitHub Actions permissions.
  - Added `ready_for_review` pull request trigger.
  - Fixed cross-platform PowerShell `LASTEXITCODE` handling after ripgrep no-match pass.
  - Aligned Android CI with the JDK 21 release verification baseline.
  - Remote `main` checks were green on `e552d03`:
    - `Release Claim Check`: success
    - `Android`: success
  - Follow-up issues:
    - #18 M9.4 release candidate readiness checklist
    - #19 Android manifest and permissions documentation
    - #20 RELEASE_REAL wording audit
    - #21 APK checksum and artifact verification
    - #22 CI lessons from M9.3

## Next

### M9.4 - Release Candidate Readiness Gate

Objective: create a release candidate readiness gate that verifies build artifacts, release notes, Android permissions, `RELEASE_REAL` boundaries, APK checksums, and claim-safe documentation before tagging or publishing an alpha release.

First tasks:

- Define `docs/RELEASE_CANDIDATE_CHECKLIST.md`.
- Add manual workflow `.github/workflows/release-candidate.yml`.
- Generate APK SHA-256 checksum.
- Upload checksum as artifact.
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
