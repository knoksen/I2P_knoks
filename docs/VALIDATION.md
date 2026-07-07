# Validation

This document lists the local validation commands for I2P Knoks Browser and I2P Connect work.

## Required Claim Check

Run this for every docs, product, app, security, onboarding, or release-facing change:

```powershell
.\scripts\check-release-claims.ps1
```

The claim check verifies required governance files and scans release-facing surfaces for clearly unsupported wording. It is a pattern-based guardrail, not a semantic proof that every claim is correct. See [Claims Register](CLAIMS_REGISTER.md) and [Claim-Safe Writing](CLAIM_SAFE_WRITING.md) for the authoritative policy.

## Local Release Verification

Run this before completing release-facing, app, security, onboarding, or product-pack changes:

```powershell
.\scripts\local-release-verify.ps1
```

The local verifier:

- finds Java 21 or newer
- sets Android SDK environment defaults
- runs the release-claim check
- runs Android unit tests
- builds the debug APK

## Android Real-Alpha Core-Flow Matrix

The authoritative matrix is [Android Real-Alpha Test Matrix](ANDROID_REAL_ALPHA_TEST_MATRIX.md).

Test layers:

- Pure logic tests: validation, status mapping, parser, sanitizer, and request/response classification.
- Repository and persistence tests: entity mapping, DAO/repository behavior where isolated storage is available, and migration-path evidence.
- Protocol and parser contract tests: SAM and HTTP proxy behavior with deterministic fakes.
- Android UI tests: Robolectric/Compose or instrumentation tests for principal UI representation.
- Simulated integration tests: local fake probes, fake SAM connections, and fake HTTP transports.
- External-router tests: optional manual checks that require a local I2P or i2pd router.
- Manual exploratory validation: documented checks for flows that are not stable enough for default automation.

Local command sequence for this matrix:

```powershell
.\scripts\test-release-claims.ps1
.\scripts\test-clear-generated-android-build.ps1
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*AppDatabaseMigrationTest"
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*I2pEndpointConfigTest"
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*I2pDiagnosticsClientTest"
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*I2PViewModelCoreFlowTest"
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*LogSanitizerTest"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat test
.\scripts\check-release-claims.ps1
.\scripts\local-release-verify.ps1
```

Endpoint contract tests cover canonical host normalization, IPv4/IPv6 loopback inputs, control-character rejection, invalid ports, persistence round-trip, missing settings, and malformed persisted settings.

Diagnostic contract tests cover injected fake transports, bounded policy validation, connection timeout category mapping, response timeout category mapping, cooperative cancellation, fixture cleanup after cancellation, and latest-request-wins ViewModel state.

Room migration guard tests cover the explicit migration graph, committed schema files, migration SQL shape, and the absence of destructive migration fallback in the current database builder.

Current default automated coverage uses JVM and Robolectric tests. It does not require public I2P availability, live `.i2p` destinations, public SAM bridges, public HTTP proxies, or third-party services. SAM, HTTP proxy, and router diagnostic tests use deterministic fakes and injected transports. The current diagnostic contract tests do not bind local loopback sockets; if a future test adds sockets, it must bind only to loopback, use ephemeral ports, and close fixtures reliably.

## Room Migration Validation

The authoritative migration contract is [Room Migration Contract](ROOM_MIGRATIONS.md).

Schema export path:

```text
app/schemas/no.knoksen.i2pbrowser.data.AppDatabase/
```

Run the JVM schema and graph guard:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests "*AppDatabaseMigrationTest"
```

Run the instrumentation migration suite when an Android emulator or device is available:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=no.knoksen.i2pbrowser.AppDatabaseMigrationInstrumentedTest"
```

The migration instrumentation suite uses Room `MigrationTestHelper` and executes supported old-version database paths with synthetic fixtures. It does not require a live I2P router, public `.i2p` sites, public SAM bridges, public HTTP proxies, external endpoints, or secrets.

Instrumentation requirements:

- Android SDK platform/build tools compatible with the project.
- A connected Android device or emulator.
- CI uses the Android emulator job in `.github/workflows/android.yml`.
- Local machines without an emulator/device can still compile the instrumentation APK with:

```powershell
.\gradlew.bat --no-daemon assembleDebugAndroidTest
```

Useful Room migration output locations:

- JVM guard reports: `app/build/reports/tests/testDebugUnitTest/`
- Instrumentation reports: `app/build/reports/androidTests/connected/`
- Instrumentation raw results: `app/build/outputs/androidTest-results/connected/`

When intentionally adding a new database version:

1. Increment `AppDatabase` version.
2. Add a migration from the previous supported version.
3. Export only the new current schema.
4. Retain all historical schema files.
5. Add adjacent and supported-origin-to-current migration tests.
6. Update [Room Migration Contract](ROOM_MIGRATIONS.md), [Android Real-Alpha Test Matrix](ANDROID_REAL_ALPHA_TEST_MATRIX.md), and claim evidence only when justified.

Do not rewrite historical schema files to make a migration pass.

Android instrumentation tests outside the migration suite require a device or emulator and are not part of the default local release verifier. Run them only when an emulator/device is available:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

External-router validation is optional and manual. It requires a local I2P or i2pd router and must not be described as automated CI evidence unless the environment is explicitly declared.

Useful output locations:

- JVM test reports: `app/build/reports/tests/testDebugUnitTest/`
- Debug APK: `app/build/outputs/apk/debug/`
- Instrumentation reports, when run: `app/build/reports/androidTests/connected/`

Known validation notes:

- Gradle may print Compose or Android API deprecation warnings. Treat new warnings as review signals, but do not claim validation failed unless the command exits non-zero.
- Git may print CRLF normalization warnings on Windows during `git diff --check`; these are not whitespace errors by themselves.
- If `app/build` is locked on Windows, use the safe generated-build recovery sequence below.

## Safe Windows Generated-Build Recovery

Use this helper only when validation encounters the known Windows lock or access-denied condition under the generated Android build directory:

```text
app/build
```

Recommended sequence:

```powershell
.\scripts\clear-generated-android-build.ps1 -DryRun
.\scripts\clear-generated-android-build.ps1 -StopGradle
.\scripts\local-release-verify.ps1
```

The dry run performs the same repository and path checks and prints the exact canonical path that would be removed. `-StopGradle` invokes only the repository wrapper, `.\gradlew.bat --stop`, from the verified repository root before cleanup.

Safety properties:

- derives one fixed target: `<repository-root>\app\build`
- verifies repository identity with multiple markers
- canonicalizes repository, app, and target paths
- refuses unexpected roots or targets
- refuses cleanup when links, junctions, or reparse points make the deletion boundary ambiguous
- removes generated build output only
- does not use broad Git cleanup or broad Gradle cleanup as a fallback

The helper does not:

- modify source code
- reset Git changes
- remove untracked project files
- remove Gradle caches outside `app\build`
- repair arbitrary Gradle failures
- guarantee that every Windows file lock can be released
- terminate unrelated Java processes

If cleanup still fails with access denied or a held lock:

- close Android Studio windows using the project
- close File Explorer windows or terminals holding handles under `app\build`
- rerun `.\scripts\clear-generated-android-build.ps1 -StopGradle`
- retry the dry run and cleanup
- inspect the owning process with an appropriate local administrative tool

Do not terminate all Java processes indiscriminately, and do not replace this helper with broad cleanup commands.

## Additional Useful Checks

Use these when they match the change:

```powershell
git diff --check
.\scripts\test-release-claims.ps1
.\scripts\test-clear-generated-android-build.ps1
.\gradlew.bat testDebugUnitTest
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Run `.\scripts\test-release-claims.ps1` when changing the claim checker or claim-safe wording policy.
Run `.\scripts\test-clear-generated-android-build.ps1` when changing the generated-build recovery helper, validation docs, or build-lock guidance.
Run `.\gradlew.bat testDebugUnitTest` or `.\gradlew.bat test` when changing Android source, core-flow tests, parser behavior, endpoint validation, persistence, UI copy, or test-matrix docs.
Run the targeted `*I2pEndpointConfigTest`, `*I2pDiagnosticsClientTest`, `*I2PViewModelCoreFlowTest`, and `*LogSanitizerTest` filters when changing endpoint contracts, router diagnostic contracts, timeout/cancellation behavior, stale-result handling, or diagnostic log sanitization.

For release-candidate review, also use `docs/RELEASE_CANDIDATE_CHECKLIST.md`.

## Failure Reporting

If validation cannot run, record:

- command
- exit code
- relevant error text
- likely blocker
- last successful narrower command

Do not claim validation passed unless the command completed successfully.
