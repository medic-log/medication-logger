---
name: release
description: Publish a new version of the app as a GitHub release - bump the version, commit, tag and push so the release workflow builds and posts the signed APK. Use when asked to release, cut a version, or publish the app.
argument-hint: "[version, e.g. 0.2.0]"
disable-model-invocation: true
allowed-tools: Bash(.claude/skills/release/bump.sh:*), Bash(git status:*), Bash(git log:*), Bash(git diff:*), Bash(git tag:*)
---

# Release a new version

Pushing a `v<versionName>` tag runs `.github/workflows/release.yml`, which builds the release APK signed with the repo secrets and creates the GitHub release with generated notes. This skill prepares and pushes that tag.

## 1. Pick the version

Use the version in the arguments (`$ARGUMENTS`). If none was given, read the current `versionName` in `app/build.gradle.kts`, look at `git log <last tag>..HEAD --oneline` (or all commits if there's no tag), propose the next semver version (patch for fixes, minor for features), and ask the user to confirm it before going on.

## 2. Bump and verify

Run `.claude/skills/release/bump.sh <version>`. It:

- Stops with exit 2, changing nothing, if not on `master`, the tree is dirty, the tag exists, `origin/master` is ahead, or `signing.env` is missing. Report the reason to the user; don't fix it on your own (don't stash, pull or commit their changes).
- Runs `scripts/check.sh` (build, lint, unit tests, as CI does) and stops with exit 3, changing nothing, if it fails. Never commit, tag or push in that case: report the lint errors and failed tests it lists. Fixing them is a separate change for the user to commit (see the `/check` skill), after which the release can be retried.
- Doesn't bump if `versionName` already equals the version (only possible when it was never tagged, e.g. the first release); then the release commit is the current `HEAD`.
- Exits 1 on a bad or older version, or if the signed build fails. On a build failure, revert the bump with `git checkout app/build.gradle.kts` and report the error.
- Otherwise sets `versionName`, increments `versionCode` and builds a signed release APK locally, so the CI release won't fail on build or signing.

## 3. Commit, tag, push

The user invoking this skill with a version is the go-ahead to publish; if you proposed the version, their confirmation is.

```bash
git commit -am "Release v<version>"   # plus the Co-Authored-By trailer
git tag -a v<version> -m "v<version>"
git push origin master v<version>
```

If the bump wasn't needed, skip the commit and tag `HEAD`.

If the push fails, leave the commit and tag in place and tell the user what failed; don't force-push or delete the tag.

## 4. Report

Tell the user the release workflow is running at https://github.com/medic-log/medication-logger/actions and the release will appear at https://github.com/medic-log/medication-logger/releases/tag/v<version>. If it fails, the usual causes are missing or wrong `SIGNING_*` repo secrets (see CONTRIBUTING.md, CI and releases).
