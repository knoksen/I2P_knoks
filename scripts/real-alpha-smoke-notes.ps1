$ErrorActionPreference = "Stop"

Write-Host "Real Alpha Smoke Notes" -ForegroundColor Cyan
Write-Host "1. Build/install debug APK"
Write-Host "2. Configure endpoint"
Write-Host "3. Run diagnostics"
Write-Host "4. Connect SAM session"
Write-Host "5. Inspect .i2p URL"
Write-Host "6. Stop router and confirm failure remains honest"
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Cyan
Write-Host "adb devices"
Write-Host "adb install -r app\build\outputs\apk\debug\app-debug.apk"
Write-Host "adb shell am start -n no.knoksen.i2pbrowser/.MainActivity"
Write-Host "adb logcat | findstr /i `"I2P SAM PROXY`""
