# Release Candidate Readiness Checklist

## Purpose

This checklist is the manual release-candidate gate for real-alpha review. It collects the existing M9.3 release-claim gate and M9.4 hardening checks into one review process before tagging or publishing a real-alpha candidate.

## Scope

This checklist verifies:

- release-facing wording
- Android permission boundary
- `RELEASE_REAL` UI boundaries
- debug APK artifact and checksum
- known limits and non-goals
- real-device or emulator smoke testing
- release notes readiness

## Non-goals

- No production signing
- No Play Store publishing
- No anonymity guarantees
- No VPN or Tor claims
- No embedded-router claim
- No full browser isolation claim
- No new runtime behavior

## Required Source State

- [ ] Working tree is clean
- [ ] Branch is up to date with `main`
- [ ] Release candidate commit SHA recorded
- [ ] No generated build artifacts committed

## Required Local Verification

- [ ] `./scripts/check-release-claims.ps1`
- [ ] `git diff --check`
- [ ] `./scripts/local-release-verify.ps1`

If local verification hits the known generated `app/build` lock on Windows, run `.\scripts\clear-generated-android-build.ps1 -DryRun`, then `.\scripts\clear-generated-android-build.ps1 -StopGradle`, and rerun verification. Do not use broad cleanup.

## Required Remote Verification

- [ ] `Release Claim Check`: success
- [ ] `Android`: success
- [ ] Unit tests: success
- [ ] `assembleDebug`: success
- [ ] Debug APK upload: success
- [ ] Debug APK checksum upload: success
- [ ] Workflow summary includes SHA-256

## Artifact Verification

Reference `docs/RELEASE_PROCESS.md`.

- [ ] Download `i2p-knoks-debug-apk`
- [ ] Download `i2p-knoks-debug-apk-sha256`
- [ ] Verify checksum with `sha256sum -c app-debug.apk.sha256`
- [ ] Verify checksum with PowerShell on Windows if reviewing on Windows
- [ ] Do not install or distribute APK if hashes mismatch

## Permission Boundary Review

Reference `docs/ANDROID_PERMISSIONS.md`.

- [ ] `AndroidManifest.xml` permissions match documented permission boundary
- [ ] No unexpected VPN service permission
- [ ] No unexpected storage/location/camera/microphone permission
- [ ] README permission summary still matches manifest
- [ ] Security boundaries still link to permission boundary

## Security Boundary Review

Reference `docs/SECURITY_BOUNDARIES.md`.

- [ ] External I2P/i2pd router requirement remains documented
- [ ] No OS-level VPN claim
- [ ] No Tor integration claim
- [ ] No audited encrypted chat claim
- [ ] No embedded router claim
- [ ] No full browser isolation claim
- [ ] No unsupported traffic protection claim

## RELEASE_REAL UI Review

Reference the #20 wording audit and current source.

- [ ] `RELEASE_REAL` hides or excludes lab/simulation-only surfaces
- [ ] Lab/simulation UI is clearly labeled when present
- [ ] Router/status UI uses measured connection/endpoint wording
- [ ] Browser/page inspection UI uses preview/inspection wording
- [ ] No UI text implies unsupported anonymity, VPN, Tor, encrypted messaging, or embedded-router behavior

## Release Notes Review

- [ ] Release notes include real features
- [ ] Release notes include known limits
- [ ] Release notes include non-goals
- [ ] Release notes include APK checksum instructions
- [ ] Release notes avoid unsupported privacy/security guarantees

## Real-Alpha Smoke Test

- [ ] App launches
- [ ] Router diagnostics screen opens
- [ ] Configured endpoint status is displayed
- [ ] Proxy/SAM checks are bounded to configured external endpoints
- [ ] Browser/page inspector shows local preview or real proxy result with clear state
- [ ] `RELEASE_REAL` does not expose lab-only route visualizer, telemetry, optimizer, or peer-discovery surfaces

## Final Signoff

- [ ] RC commit SHA:
- [ ] Android workflow run:
- [ ] Release Claim Check workflow run:
- [ ] APK SHA-256:
- [ ] Reviewer:
- [ ] Date:
