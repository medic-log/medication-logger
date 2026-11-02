#!/usr/bin/env bash
# Builds the debug APK incrementally, then installs and launches it on the connected device.
# Exit codes: 0 launched, 1 build/install/launch failed, 2 no device (built only), 3 device not responding.
set -euo pipefail
cd "$(dirname "$0")/../../.."

PKG=com.mediclog.medicationlogger
APK=app/build/outputs/apk/debug/app-debug.apk
ADB_TIMEOUT=${ADB_TIMEOUT:-90}

# The SDK's adb, not whatever is on PATH: a different adb version (e.g. a distro package) kills
# Android Studio's adb server on startup and the two fight over port 5037, wedging installs
sdk_dir=$(sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null)
sdk_dir=${sdk_dir:-${ANDROID_HOME:-$HOME/Android/Sdk}}
adb() { "$sdk_dir/platform-tools/adb" "$@"; }

# Same Gradle project and outputs as Android Studio, so only what changed since either side's last build is rebuilt
./gradlew assembleDebug --quiet

mapfile -t devices < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if (( ${#devices[@]} == 0 )); then
    echo "Built $APK. No device connected, so nothing was installed or launched."
    exit 2
fi
serial=${ANDROID_SERIAL:-${devices[0]}}
if (( ${#devices[@]} > 1 )) && [[ -z ${ANDROID_SERIAL:-} ]]; then
    echo "Several devices connected, using $serial (set ANDROID_SERIAL to pick another)."
fi

# A wedged adb connection hangs forever instead of failing, so every call gets a timeout
adb_() { timeout "$ADB_TIMEOUT" "$sdk_dir/platform-tools/adb" -s "$serial" "$@"; }

if ! timeout 10 "$sdk_dir/platform-tools/adb" -s "$serial" shell true; then
    echo "Device $serial is listed but not responding to adb. Unplug and replug USB, or run: adb kill-server"
    exit 3
fi

# Not ./gradlew installDebug: its ddmlib times out reading properties on some devices (Samsung)
adb_ install -r "$APK"
adb_ shell am start -n "$PKG/.MainActivity"
echo "Launched $PKG on $serial."
