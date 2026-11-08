# Medication logger

Logs your medication prescriptions, pill boxes, and weekly refills, with reminders to take your pill, refills, pickups, running out, and expiry. Everything stays on your device.

## Using the app

The app opens on **Boxes**, and has four tabs:

- **Settings:** a **Reminders** section, then a **Settings** section with **Help**, **Language**, **Dates**, **Update**, **Share**, **Import** and **Export** buttons. **Language** switches the app between English, Spanish, Portuguese and Dutch; it starts in English and doesn't follow the phone's language. **Dates** picks how dates are shown everywhere, notifications included: 2026-11-14 (the default), 14/11/2026, 11/14/2026, or as the chosen language writes them (Nov 14, 2026 in English). **Help** opens the tutorial, which also opens the first time you use the app: it fills the tabs with sample records (your own data is untouched), starts by asking for your language, dims the app and highlights what each step explains, goes through each tab and asks for your daily pill time and weekly refill day and time. Closing it, by finishing or skipping, goes to Settings: to the top the first time, or back to where you tapped **Help**. **Export** saves your prescriptions, boxes, refills, and missed and delayed doses to a JSON file, without settings (the app is left out of Android's backups and transfers to a new phone, as it holds health data, so this is how you move your records); **Import** replaces them with those in such a file, after asking before and after you pick it, and keeps your settings. **Update** checks the download site for a newer version and, if there is one, asks to update, downloads its APK in the app, checks it against the site's SHA-256 and asks to install it (the first time, Android asks you to allow installs from the app). The app also checks on its own each time it starts and, if there is a newer version, offers it in a dialog where you choose to update or not now. **Share** shares the download site's address. Reminders all start on, so turn off the ones you don't want. Daily pill: every day at a chosen time. Refill: weekly on a chosen weekday and time. Pickup: a number of days after ordering. Running out: a number of days before the pills you've bought run out, to get your next repetition, or a new prescription when none are left. Expiry: a number of days before a box with pills left expires. It also shows the estimated date all your medication runs out, repetitions not bought included, at one pill a day. Times are entered in 24-hour format. Reminder times keep the UTC offset they were set at: after a move to a zone with another offset, or a change to or from summer time, reminders still arrive at the same moments, and when you next open the app it moves the daily pill and refill times to the new offset and shows a dialog with the old and new offsets and the new times. A new zone at the same offset changes nothing.
- **Prescriptions:** add a prescription with **New prescription**, whose fields start from your previous one. **Order repetition** records an order, and **Buy repetition** asks each box's expiry date and adds that repetition's boxes.
- **Boxes:** **Weekly refill** takes 7 pills from a box, or what's left if it has fewer. When your weekly lot (shown on the Refills tab) won't last until your next weekly refill, **Partial refill** appears: use it on the next box to make up the difference. **Save as extra** sets a box aside until you bring it **Back to inventory**; while it's extra, its card has a notes field, for what it's kept for, which going back to the inventory clears.
- **Refills:** the log of your refills, with partial refills made on one day that add up to a full week joined into one stack of cards. Register a dose with **Log missed dose** or **Log delayed dose**, which asks for the time you took it. Below them is how many pills are currently in your weekly lot: each refill adds its pills, each day's pill time takes one, and a missed dose puts one back. A missed or delayed dose joins the stack of the refill made before its day; a missed one puts its pill back in your weekly lot, so your medication lasts a day longer.

Prescriptions and boxes can be filtered to **Active** or **All**, and boxes also to **Extra**. The icons on boxes and refills open the prescription or box they belong to.

Hold a prescription, box or refill card for a second to edit its fields by hand or delete it, after confirming. Deleting a prescription or box can keep its boxes or refills; their link to it then shows crossed out and disabled.

## Web app

The same app runs in the browser at <https://medic-log.github.io/medication-logger/app/>, on any phone, tablet or computer, and installs from there: **Install web app** on the download page, or the browser's own install or Add to Home Screen. Once opened it also works offline. It looks and works like the Android app, in the same four languages, except where the browser differs:

- Your records are kept in that browser on that device. **Export** and **Import** move them between browsers and to or from the Android app; both apps use the same file.
- Reminders arrive as notifications while the app is open, also in the background, once you allow notifications (asked when the tutorial closes, or from the notice in Settings). They can't arrive while it's closed. A pickup, running-out or expiry reminder that's due shows the next time the app is open after 9:00.
- **Update** reloads into the newest version; the app also checks each time it starts and offers it.
- **Share** uses the browser's share sheet, or copies the link where there's none.
- Hold a card for a second to edit it, as on Android; on a computer a right-click does it too.

## Build from source

Requirements: JDK 17+ and the Android SDK (platform 37). Android Studio works too: open the project folder.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The web app needs Node.js 22 or newer:

```bash
cd web
npm ci
npm run dev        # http://localhost:5173/medication-logger/app/
```

Checks, CI and releases are described in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The code is under the [MIT license](LICENSE).
