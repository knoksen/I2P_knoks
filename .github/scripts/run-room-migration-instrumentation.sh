#!/usr/bin/env bash
set -uo pipefail

REPORT_DIR="app/build/reports/room-migration-ci"
mkdir -p "$REPORT_DIR"

adb devices -l | tee "$REPORT_DIR/adb-devices.txt"
adb shell getprop sys.boot_completed | tee "$REPORT_DIR/boot-completed.txt"
adb shell getprop ro.build.version.release | tee "$REPORT_DIR/android-release.txt"
adb shell getprop ro.product.cpu.abi | tee "$REPORT_DIR/android-abi.txt"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c

test_exit=0
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=no.knoksen.i2pbrowser.AppDatabaseMigrationInstrumentedTest 2>&1 | tee "$REPORT_DIR/gradle-output.txt" || test_exit=$?
adb logcat -d > "$REPORT_DIR/logcat.txt" || true

exit "$test_exit"
