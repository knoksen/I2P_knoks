$ErrorActionPreference = "Stop"

$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:USERPROFILE\Android\Sdk" }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Write-Host "Checking release-facing claims..." -ForegroundColor Cyan
& "$PSScriptRoot\check-release-claims.ps1"

Write-Host "Running Android release verification..." -ForegroundColor Cyan
& "$PSScriptRoot\..\gradlew.bat" clean testDebugUnitTest assembleDebug

Write-Host "Local release verification passed." -ForegroundColor Green
