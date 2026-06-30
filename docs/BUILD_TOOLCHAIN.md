# Build Toolchain

I2P Knoks Browser currently requires Java 21 or newer for local release verification.

## Why Java 21

The Android unit test flow uses Robolectric with the project's configured Android SDK. In this setup, the Robolectric sandbox requires Java 21. A Java 17 runtime can compile parts of the project, but it can fail during `testDebugUnitTest`.

## Recommended Windows Setup

Install Temurin JDK 21:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

The local verification script searches for:

- current `JAVA_HOME`, if set
- Temurin JDK 21 under `C:\Program Files\Eclipse Adoptium\`
- Android Studio JBR only if it exists and passes the Java 21+ preflight

## Required Android Tools

The Android SDK is still required. By default, the verification script uses:

```powershell
$env:ANDROID_HOME = "$env:USERPROFILE\Android\Sdk"
```

Override `ANDROID_HOME` before running the script if your SDK is installed elsewhere.

## Verify Locally

```powershell
.\scripts\local-release-verify.ps1
```

The script prints:

- detected `JAVA_HOME`
- `java -version`
- `javac -version`
- release claim check result
- Gradle `clean testDebugUnitTest assembleDebug` result
