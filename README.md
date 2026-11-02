# Medication logger

Logs your medication prescriptions, pill boxes, and weekly refills, with reminders for refills, pickups, and running out.

## Build from source

Requirements: JDK 17+ and the Android SDK (platform 37). Android Studio works too: open the project folder.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Checks, CI and releases are described in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The code is under the [MIT license](LICENSE).
