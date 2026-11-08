# Contributing

See the [README](README.md) for how to build the app.

## Checks

`./scripts/check.sh` runs what CI runs: the debug build, Android lint and the JVM unit tests (`app/src/test`, using Robolectric), then, once `web/node_modules` is installed (`cd web && npm ci`), the web app's unit tests and build. On failure it lists the lint errors and failed tests.

The web app's browser tests (`web/e2e`, Playwright) run the user flows against the production build: `cd web && npm run e2e`. They use Playwright's Chromium (`npx playwright install chromium`), or one already installed with `CHROMIUM_PATH=/usr/bin/chromium-browser npm run e2e`.

The pre-push hook runs it before every push. Project setup enabled it for this clone; on a new clone, enable it once:

```bash
git config core.hooksPath .githooks
```

This also works when pushing from Android Studio. Skip it for one push with `git push --no-verify`, or untick **Run Git hooks** in Android Studio's push dialog.

## Project structure

```
app/src/main/java/com/mediclog/medicationlogger/
├── MainActivity.kt                 # Hosts the UI
├── MedicationLoggerApplication.kt  # Shared objects; keeps alarms in step with the data
├── data/                           # The records and their actions, reminder dates, storage
├── reminders/                      # Alarms and notifications
└── ui/
    ├── main/                       # The tab frame and links between tabs
    ├── reminders/                  # One folder per tab: its screen and ViewModel
    ├── prescriptions/
    ├── boxes/
    ├── refills/
    ├── components/                 # Shared UI pieces
    └── theme/                      # Material 3 theme
app/src/test/java/com/mediclog/medicationlogger/  # JVM unit tests
web/                                            # The web app (React, TypeScript, Vite, installable PWA)
├── android-resources.ts                        # Reads the Android strings, icons and version at build time
├── src/data/                                   # Port of app/.../data, with its tests ported alongside
├── src/reminders/                              # Notifications while the app is open
├── src/i18n/                                   # Strings and plurals; webStrings.ts holds the web-only texts
├── src/update/                                 # Updates through the service worker, installing
├── src/ui/                                     # Same layout as app/.../ui
├── e2e/                                        # Browser tests, ports of MainActivityTest
└── scripts/icons.mjs                           # Draws the web icons from the launcher icon
site/                                           # The download page and feedback form
gradle/libs.versions.toml                       # Dependency versions
scripts/check.sh                                # CI checks, also run by .githooks/pre-push
```

## CI and releases

GitHub Actions runs `scripts/check.sh` on every push to `master` and every pull request (`.github/workflows/ci.yml`), and in a second job the web app's unit tests, build and browser tests. The APK and the lint and test reports are attached to the run as artifacts.

Pushing a `v*` tag builds a signed release APK and publishes it as a GitHub release (`.github/workflows/release.yml`), both versioned and as `medication-logger.apk`, so the newest is always at `https://github.com/medic-log/medication-logger/releases/latest/download/medication-logger.apk`. It then deploys the download site to GitHub Pages at `https://medic-log.github.io/medication-logger/`: the page in `site/index.html`, the APK as `medication-logger.apk`, `latest.json` (`versionCode`, `versionName`, `apk`, `sha256`) and the web app, built from `web/`, under `app/`. The web app takes its version from `app/build.gradle.kts`, so each release ships both apps at the same version. Pages must be enabled once with Source set to GitHub Actions (Settings → Pages). The release workflow first runs CI (`ci.yml`, both jobs) on the tagged commit and builds and publishes nothing if it fails; `/release` also runs `./scripts/check.sh` before tagging, to catch failures before the push. The tag must match `versionName` in `app/build.gradle.kts`:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Release signing needs a keystore, stored as repository secrets (Settings → Secrets and variables → Actions). Create one once and keep it safe: updates to an installed app must be signed with the same key.

```bash
keytool -genkeypair -v -keystore release.jks -alias release -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # value for SIGNING_KEYSTORE_BASE64
```

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | The keystore, base64-encoded |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias (`release` above) |
| `SIGNING_KEY_PASSWORD` | Key password |

To sign a release build locally, set the same variables, with `SIGNING_KEYSTORE_FILE` pointing at the keystore instead of the base64 value, and run `./gradlew assembleRelease`. A gitignored `signing.env` in the project root can hold them all, and `/release` requires it:

```bash
set -a; . ./signing.env; set +a
./gradlew assembleRelease
```
