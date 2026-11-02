# Contributing

See the [README](README.md) for how to build the app.

## Checks

`./scripts/check.sh` runs what CI runs: the debug build, Android lint and the JVM unit tests (`app/src/test`, using Robolectric). On failure it lists the lint errors and failed tests.

The pre-push hook runs it before every push. Project setup enabled it for this clone; on a new clone, enable it once:

```bash
git config core.hooksPath .githooks
```

This also works when pushing from Android Studio. Skip it for one push with `git push --no-verify`, or untick **Run Git hooks** in Android Studio's push dialog.

## Project structure

```
app/src/main/java/com/mediclog/medicationlogger/
├── MainActivity.kt   # Hosts the UI
├── data/             # Repositories and data sources (add when needed)
└── ui/
    ├── <feature>/    # A feature's screen and ViewModel, e.g. home/
    └── theme/        # Material 3 theme
app/src/test/java/com/mediclog/medicationlogger/  # JVM unit tests
gradle/libs.versions.toml                       # Dependency versions
scripts/check.sh                                # CI checks, also run by .githooks/pre-push
```

## CI and releases

GitHub Actions runs `scripts/check.sh` on every push to `master` and every pull request (`.github/workflows/ci.yml`). The APK and the lint and test reports are attached to the run as artifacts.

Pushing a `v*` tag builds a signed release APK and publishes it as a GitHub release (`.github/workflows/release.yml`), both versioned and as `medication-logger.apk`, so the newest is always at `https://github.com/medic-log/medication-logger/releases/latest/download/medication-logger.apk`. The release workflow doesn't run the checks, so run `./scripts/check.sh` before tagging, or use `/release`, which does. The tag must match `versionName` in `app/build.gradle.kts`:

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
