# CLAUDE.md

Logs your medication prescriptions, pill boxes, and weekly refills, with reminders for refills, pickups, and running out. Single-module Android app (`:app`, package `com.mediclog.medicationlogger`), Kotlin + Compose, minSdk 28, target SDK 37. README.md is the user-facing spec.

Detailed standards load automatically from `.claude/rules/` when you work on matching files: Kotlin and architecture, UI and design, tests, Gradle.

## Working efficiently

- **Replies:** lead with the result, in as few words as it takes. Don't restate the request, narrate steps, recap the diff or offer more work. Report what failed plainly.
- **Decisions:** take the conventional default and state it. Ask only when the answer would change the outcome and you can't find it out yourself.
- **Scope:** do what was asked with the smallest change. Reuse existing helpers, remove what the change leaves unused, and mention other problems instead of fixing them.
- **Reading:** open only the files the change touches (the map below says where things are). Don't explore the codebase, read README.md or re-read files you just edited.
- **Running:** never install or launch the app unless asked; the user runs `/run`. Run `./scripts/check.sh` once when the change is done, not after every edit. On success it prints at most the lint report path.
- **Docs:** update this file or README.md only when user-visible behavior or architecture changes.
- **Git:** commit or push only when asked.
- Machine-specific notes (SDK path, test device) go in the gitignored `CLAUDE.local.md`.

## Commands

- `./scripts/check.sh`: build, lint and unit tests, as CI and the pre-push hook do (`/check` runs it).
- `./gradlew testDebugUnitTest --tests '*MainActivityTest'`: one test class.
- Releases: `/release` (bumps the version, tags, pushes; `release.yml` signs and publishes).

## Map and non-obvious constraints

Keep this to what isn't obvious from the code; add an entry when a new file or constraint needs one.

- `MainActivity`: hosts the Compose UI. `MedicationLoggerApplication`: holds the repository, clock and scheduler, and keeps alarms in step with the data.
- `ui/<feature>/`: a feature's screen and ViewModel (`main/` is the tab frame; `reminders/` (the Settings tab), `prescriptions/`, `boxes/`, `refills/` are the tabs, in that order). `ui/AppViewModelProvider`: creates the ViewModels. `ui/components/`: shared pieces. `ui/theme/`: the fixed dark salmon red theme, deliberately without dynamic color.
- `data/MedicationData`: every table and action, as pure functions. `MedicationRepository` applies them with today's date and saves. `JsonFileMedicationStore`: one JSON file, no database.
- `data/ReminderCalculator` + `reminders/`: alarms are one per reminder kind and derived from the data, with nothing stored about what has fired. So pickup, run-out and expiry reminders repeat daily until resolved, and everything is recomputed after a reboot.
- A box's status is computed from `pillsLeft` and `isExtra`, not stored. `MedicationData.lotPills` (the pills set out for the week) isn't stored either: it replays refills (+amount), missed doses (+1) and each day's pill time (-1) from the first refill, counting dated records as made after that day's pill. Partial refill tops it up to last until the next refill, and the run-out estimate starts from it.
- Tests: `MainActivityTest` runs tablet-sized, because under Robolectric a Material text field in a dialog never goes idle at some window sizes, including 412x915 (it fails after a 60 s timeout, with `AppNotIdleException`).
