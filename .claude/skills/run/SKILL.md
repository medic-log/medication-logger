---
name: run
description: Build, install and launch the app on the connected Android device, then check it's running.
disable-model-invocation: true
allowed-tools: Bash(.claude/skills/run/run.sh:*), Bash(adb devices:*), Bash(adb logcat:*), Bash(adb shell dumpsys activity activities:*), Bash(adb exec-out screencap:*)
---

# Run the app on a device

Run `.claude/skills/run/run.sh` straight away, without asking for confirmation. It:

1. Builds incrementally with `./gradlew assembleDebug`. It shares outputs with Android Studio, and configuration/build caching is on in `gradle.properties`, so an unchanged project builds in about a second.
2. Stops if no device is connected (exit 2): the APK is built but nothing is installed or launched. Tell the user, and don't retry.
3. Stops if the device is listed but adb doesn't respond (exit 3). Tell the user to replug USB or run `adb kill-server`; don't restart the adb server yourself, since other sessions may be using it.
4. Otherwise installs with `adb install -r` and launches `MainActivity`. With several devices it uses the first one, or `ANDROID_SERIAL` if set.

On a build failure (exit 1 before install), fix the compile error and run the script again.

## After launching (only when checking behavior)

- Crashes: `adb logcat -d -b crash`.
- Screen: `adb exec-out screencap -p > <scratchpad>/screen.png`, then read the image.

## Caveats

- Never uninstall the app or clear its data without asking.
