$ErrorActionPreference = "Stop"

function Get-JavaMajorVersion {
    param([string]$JavaExe)

    $versionOutput = & $JavaExe -version 2>&1
    $versionLine = ($versionOutput | Select-Object -First 1).ToString()
    if ($versionLine -match '"(?<version>[0-9]+)(\.|")') {
        return [int]$Matches.version
    }
    throw "Could not parse Java version from: $versionLine"
}

function Test-JavaHome {
    param([string]$JavaHome)

    if ([string]::IsNullOrWhiteSpace($JavaHome)) { return $null }

    $javaExe = Join-Path $JavaHome "bin\java.exe"
    $javacExe = Join-Path $JavaHome "bin\javac.exe"
    if (-not (Test-Path $javaExe) -or -not (Test-Path $javacExe)) { return $null }

    try {
        $major = Get-JavaMajorVersion -JavaExe $javaExe
        if ($major -lt 21) { return $null }
        return [pscustomobject]@{
            JavaHome = $JavaHome
            JavaExe = $javaExe
            JavacExe = $javacExe
            Major = $major
        }
    } catch {
        return $null
    }
}

function Find-Java21 {
    $candidates = New-Object System.Collections.Generic.List[string]

    if ($env:JAVA_HOME) {
        $candidates.Add($env:JAVA_HOME)
    }

    $temurinRoot = "C:\Program Files\Eclipse Adoptium"
    if (Test-Path $temurinRoot) {
        Get-ChildItem $temurinRoot -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }

    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $androidStudioJbr) {
        $candidates.Add($androidStudioJbr)
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        $valid = Test-JavaHome -JavaHome $candidate
        if ($valid) { return $valid }
    }

    return $null
}

$java = Find-Java21
if (-not $java) {
    throw @"
Java 21 or newer is required for this project's Robolectric/Android test flow.

Install Temurin JDK 21:
winget install EclipseAdoptium.Temurin.21.JDK

Then rerun:
.\scripts\local-release-verify.ps1
"@
}

$env:JAVA_HOME = $java.JavaHome
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:USERPROFILE\Android\Sdk" }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Write-Host "Detected JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan
& "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | ForEach-Object { Write-Host $_ }
& "$env:JAVA_HOME\bin\javac.exe" -version 2>&1 | ForEach-Object { Write-Host $_ }

Write-Host "Checking release-facing claims..." -ForegroundColor Cyan
& "$PSScriptRoot\check-release-claims.ps1"

Write-Host "Running Android release verification..." -ForegroundColor Cyan
& "$PSScriptRoot\..\gradlew.bat" clean testDebugUnitTest assembleDebug

Write-Host "Local release verification passed." -ForegroundColor Green
