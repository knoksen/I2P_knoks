# CI Lessons from M9.3

## Purpose

Document the CI and local-verification lessons learned while landing the M9.3 release-claim gate and M9.4 release-candidate hardening work.

This document is operational guidance for future release-candidate work. It does not introduce new runtime behavior, new security guarantees, or new CI requirements by itself.

## Lesson 1: PowerShell, ripgrep, and LASTEXITCODE

During M9.3, `scripts/check-release-claims.ps1` printed a passing message, but the Ubuntu GitHub Actions step still exited with code `1`.

The root cause was the final no-match `rg` call. For ripgrep, exit code `1` means no matches were found. That is the expected result for a clean release-claim scan, but the value remained in `$LASTEXITCODE`. In PowerShell, a script can appear logically successful while the final native command exit code still affects the process result.

The fix was to explicitly reset `$global:LASTEXITCODE = 0` after a clean pass.

Guidance:

- Treat `rg` exit code `1` as no matches, not as failure.
- Treat `rg` exit code greater than `1` as a real failure.
- Reset `$LASTEXITCODE` after a clean pass when the script is meant to succeed.
- Do not weaken the blocked-claim pattern list to make CI pass.

## Lesson 2: JDK 21 Baseline for Android CI and Local Release Verification

During M9.3, Android unit tests failed under JDK 17. The failure appeared in Robolectric / Android SDK handling, while local release verification already used JDK 21 as the baseline.

CI was aligned to JDK 21 so the remote Android workflow and local release verification use the same Java baseline.

Guidance:

- Keep Android CI and local release verification on the same JDK baseline.
- Treat JDK drift as a release-risk issue.
- Do not silently downgrade the JDK to work around test friction.
- If Robolectric or SDK behavior changes, fix the baseline explicitly and document it.

## Lesson 3: Known Windows Gradle Daemon and Generated app/build Lock

On Windows, `local-release-verify.ps1` may hit a generated `app/build` lock. This is usually caused by a Gradle daemon or Java process holding generated build outputs.

The safe cleanup is to stop Gradle/Java and remove only generated `app/build`.

```powershell
.\gradlew --stop
```

If needed, stop the stuck Java/Gradle process from Task Manager or PowerShell.

Then remove only generated output:

```powershell
Remove-Item -Recurse -Force .\app\build
```

Then rerun:

```powershell
.\scripts\local-release-verify.ps1
```

Do not remove source files, docs, Gradle config, manifest files, or checked-in project files as part of this cleanup.

## Lesson 4: Release-Claim Gate Must Stay Strict

The release-claim gate exists to stop unsupported release-facing claims. It should not be weakened just because wording is inconvenient.

Fix docs or UI wording instead of weakening the check. Lab/simulation wording must remain clearly labeled or hidden from `RELEASE_REAL`. Any real, active, or connected wording must be backed by measured runtime state or documented limits.

Examples of wording categories to avoid:

- absolute anonymity promises
- active VPN status claims
- encrypted-messaging product claims
- tunnel-safety claims
- post-quantum safety claims
- military-style security claims
- garlic-routing success claims

Preferred wording style:

- configured endpoint
- reachable / unreachable
- probe result
- local preview
- external router required
- app-level proxy endpoint
- inspection history
- measured response

## Lesson 5: M9.4 Turns Lessons into Release-Candidate Operations

Later M9.4 work made these lessons operational:

- #19 documented Android permission boundaries.
- #20 audited `RELEASE_REAL` UI wording.
- #21 added APK checksum and artifact verification.
- #18 added the release-candidate readiness checklist and connected the manual RC workflow.

The RC checklist should be used before tagging or publishing a real-alpha candidate.

Reference:

- `docs/RELEASE_CANDIDATE_CHECKLIST.md`
- `docs/RELEASE_PROCESS.md`
- `docs/ANDROID_PERMISSIONS.md`
- `docs/SECURITY_BOUNDARIES.md`

## Future PR Checklist

Before merging release-facing CI/docs changes:

- [ ] `./scripts/check-release-claims.ps1` passes.
- [ ] `git diff --check` passes.
- [ ] `./scripts/local-release-verify.ps1` passes.
- [ ] Android CI uses the documented JDK baseline.
- [ ] No release-facing claims were added without boundary review.
- [ ] Generated build-output cleanup did not remove source/config files.
- [ ] RC checklist remains accurate.
