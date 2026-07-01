# Dependency Reports

The `Dependency Report` GitHub Actions workflow generates Gradle dependency trees for review when build files or the version catalog change.

## Reports

The workflow uploads one artifact:

- `i2p-knoks-dependency-reports`

The artifact contains:

- `debugRuntimeClasspath.txt`
- `debugUnitTestRuntimeClasspath.txt`

## When To Review

Review dependency reports when a PR changes:

- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- dependency-related workflow files

## What To Check

- unexpected new networking, browser, VPN, Tor, or crypto libraries
- unexpected Firebase or Google service dependency changes
- duplicate or surprising major versions
- test-only dependencies leaking into runtime classpaths
- runtime dependencies that contradict `docs/SECURITY_BOUNDARIES.md`

This report is a review aid. It does not replace Gradle tests, release-claim checks, or manual security review.

## Local Equivalent

Run from the repository root:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:ANDROID_HOME = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

New-Item -ItemType Directory -Force build\reports\dependencies | Out-Null
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath > build\reports\dependencies\debugRuntimeClasspath.txt
.\gradlew.bat :app:dependencies --configuration debugUnitTestRuntimeClasspath > build\reports\dependencies\debugUnitTestRuntimeClasspath.txt
```

