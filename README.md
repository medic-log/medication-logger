# Medication logger

Logs your medication prescriptions, pill boxes, and weekly refills, with reminders for refills, pickups, and running out. Everything stays on your device.

## Using the app

The app opens on **Boxes**, and has four tabs:

- **Reminders:** turn on the reminders you want. Refill: weekly on a chosen weekday and time. Pickup: a number of days after ordering. Running out: a number of days before your pills run out, to get a new prescription. It also shows the estimated run-out date, at one pill a day.
- **Prescriptions:** add a prescription with **New prescription**, whose fields start from your previous one. **Order repetition** records an order, and **Buy repetition** adds that repetition's boxes.
- **Boxes:** **Weekly refill** takes 7 pills from a box, or what's left if it has fewer. When that leaves a week incomplete, **Partial refill** is highlighted: use it on the next box to make up the difference. **Save as extra** sets a box aside until you bring it **Back to inventory**.
- **Refills:** the log of your refills. Register a dose with **Log missed dose** or **Log delayed dose**.

Prescriptions and boxes can be filtered to **Active** or **All**. The icons on boxes and refills open the prescription or box they belong to.

## Build from source

Requirements: JDK 17+ and the Android SDK (platform 37). Android Studio works too: open the project folder.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Checks, CI and releases are described in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The code is under the [MIT license](LICENSE).
