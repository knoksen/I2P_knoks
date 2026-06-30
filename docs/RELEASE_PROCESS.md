# Release Process

## Purpose

This document describes how to prepare, verify, tag, and publish real-alpha GitHub releases for I2P Knoks Browser.

The current release process delivers debug APK artifacts for real-alpha testing. It is not production signing, Play Store publishing, or a promise of anonymity.

## Release Types

| Release Type | Example Tag | Purpose |
|---|---|---|
| Infrastructure hardening | `v0.3.4-toolchain-hardening` | Build/release/toolchain reliability |
| Runtime hardening | `v0.3.6-sam-timeout-hardening` | Protocol/runtime stability |
| Feature alpha | `v0.3.5-page-inspector-v2` | Real-alpha feature improvement |
| Test harness | `v0.3.7-real-device-test-harness` | Testing and validation docs |

Use annotated tags. Tag only after `main` verification passes. Do not tag from feature branches.

## Required Checks

- Release-facing claim check must pass.
- Local release verification must pass.
- Debug APK build must pass.
- Unit tests must pass.
- Any networking behavior change should be checked against the real-device/emulator checklist.
- Release notes must include known limits and explicit non-goals.

## Version / Tag Naming

Use descriptive annotated tags:

```text
v0.3.8-release-packaging
v0.3.9-sam-reconnect-hardening
v0.4.0-real-alpha-browser-inspector
```

Do not reuse tags. If a tag is wrong, document the correction clearly instead of silently pretending it never happened.

## Local Verification

Run from the repository root:

```powershell
git checkout main
git pull origin main
git status

.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

If the release includes device-test docs or networking changes:

```powershell
.\scripts\real-alpha-smoke-notes.ps1
```

## Release Checklist

- [ ] PR merged to `main`
- [ ] Local `main` fast-forwarded
- [ ] Working tree clean
- [ ] `.\scripts\check-release-claims.ps1` passes
- [ ] `.\scripts\local-release-verify.ps1` passes
- [ ] Real-device/emulator checklist reviewed if networking behavior changed
- [ ] Version/tag name chosen
- [ ] Annotated tag created
- [ ] Tag pushed
- [ ] GitHub Actions run checked
- [ ] Debug APK artifact downloaded or linked from the workflow run
- [ ] GitHub Release created
- [ ] Release notes include known limits and explicit non-goals

## Tag Commands

Example:

```powershell
git tag -a v0.3.8-release-packaging -m "Add real alpha APK release packaging process"
git push origin v0.3.8-release-packaging
```

Replace tag name and message per release. Do not tag from feature branches.

## GitHub Actions Artifact Flow

GitHub Actions builds the debug APK artifact. This artifact is for real-alpha testing only. It is not a production signed Play Store release.

Use the workflow run for the intended commit or tag:

1. Open the repository Actions tab.
2. Select the Android workflow run for the release commit/tag.
3. Confirm the run completed successfully.
4. Download the debug APK artifact.
5. Install it on an emulator or test device.
6. Run the real-alpha smoke checks.

## APK Artifact Checklist

- [ ] Workflow run belongs to expected commit
- [ ] Workflow run completed successfully
- [ ] Artifact name matches expected debug APK artifact
- [ ] APK installed on emulator or test device
- [ ] App opens
- [ ] Security Boundaries visible
- [ ] Diagnostics reachable

## GitHub Release Creation

Create a GitHub Release from the pushed tag. Include release notes that describe what changed, how it was verified, and what remains intentionally out of scope.

Attach or link the debug APK artifact from the matching workflow run. Confirm the artifact belongs to the intended commit/tag before publishing.

## Release Notes Template

### Summary

Short one-paragraph summary.

### Added

- ...

### Changed

- ...

### Fixed

- ...

### Verification

- `.\scripts\check-release-claims.ps1`
- `.\scripts\local-release-verify.ps1`
- Real-device/emulator checklist if applicable

### Known Limits

- No embedded I2P router
- No WebView/full browser
- No JavaScript execution
- No VPN
- No Tor
- No secure chat
- Debug APK only unless otherwise stated

### Non-goals

- Production signing
- Play Store publishing
- Anonymous browsing guarantees

You can print a starter template with:

```powershell
.\scripts\new-release-notes-template.ps1 -Tag v0.3.8-release-packaging
```

## Changelog Template

Use [Changelog Template](CHANGELOG_TEMPLATE.md) when preparing a durable changelog entry.

## Known Limits

- Debug APK artifacts are for real-alpha testing.
- Production signing keys are not part of this process.
- Play Store publishing is not part of this process.
- The app still requires an external Java I2P or i2pd router for real I2P behavior.
- The app does not provide an embedded router, OS-level VPN, Tor integration, secure chat, or full WebView browser.

## Rollback / Bad Release Handling

If a release is bad:

1. Stop promoting the release.
2. Mark the GitHub Release notes with a clear warning.
3. Open a fix PR.
4. Publish a new tag after verification.
5. Do not rewrite public history unless there is a clear repository-maintenance reason and the correction is documented.

## Final Release Signoff Checklist

- [ ] Main branch verified locally
- [ ] Working tree clean
- [ ] Tag pushed
- [ ] GitHub Actions successful for intended commit/tag
- [ ] Debug APK artifact verified
- [ ] Known limits included in notes
- [ ] Non-goals included in notes
- [ ] Real-device/emulator signoff completed when applicable
