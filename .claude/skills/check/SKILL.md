---
name: check
description: Run the same checks as CI (debug build, Android lint, JVM unit tests) and fix what fails. Use before committing or pushing, after changing code, or when CI failed.
allowed-tools: Bash(./scripts/check.sh:*), Bash(./gradlew testDebugUnitTest:*)
---

# Run the CI checks locally

Run `./scripts/check.sh` straight away. It's what CI and the pre-push hook run: `./gradlew assembleDebug lint testDebugUnitTest --continue`. Exit 0 means CI will pass. On exit 1 it ends with the lint errors and failed tests.

## On failure

- **Compile error:** fix it and rerun.
- **Lint error:** fix the cause. Don't add `@Suppress`/`tools:ignore` or a lint baseline unless the user agrees; say why the check doesn't apply. Lint warnings (e.g. newer dependency versions) don't fail CI; mention them, but don't upgrade dependencies unasked.
- **Failed test:** work out whether the code or the test is wrong before changing either. If a test's expectation is wrong, say so when reporting. To rerun one class: `./gradlew testDebugUnitTest --tests '*MainActivityTest'`.

Rerun the whole script after fixing, then report what failed and what you changed.

## About the tests

- JVM tests live in `app/src/test`. Robolectric supplies the `Application` and a fresh SharedPreferences for each test, and runs Compose UI tests.
- Time-dependent code takes an injectable clock or dispatcher, so tests use virtual time; don't use real time or `Thread.sleep`.
