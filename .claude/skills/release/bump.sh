#!/usr/bin/env bash
# Usage: bump.sh <version>   e.g. bump.sh 0.2.0
# Checks the repo is ready to release and passes the CI checks (scripts/check.sh), sets versionName to <version> and increments versionCode
# (unless versionName is already <version> and never tagged, e.g. the first release),
# then builds a signed release APK with signing.env to make sure the release workflow will succeed.
# Exit codes: 1 bad input or build failure, 2 repo not ready, 3 checks failed (nothing changed for 2 and 3).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

version=${1:-}
gradle_file=app/build.gradle.kts

if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "error: version must look like 1.2.3, got '$version'" >&2
  exit 1
fi

branch=$(git branch --show-current)
if [[ $branch != master ]]; then
  echo "error: on branch '$branch', releases are made from master" >&2
  exit 2
fi
if [[ -n $(git status --porcelain) ]]; then
  echo "error: working tree has uncommitted changes:" >&2
  git status --short >&2
  exit 2
fi
if git rev-parse -q --verify "refs/tags/v$version" >/dev/null; then
  echo "error: tag v$version already exists" >&2
  exit 2
fi
git fetch -q origin master
if [[ $(git rev-list --count HEAD..origin/master) -gt 0 ]]; then
  echo "error: origin/master has commits this branch doesn't, pull first" >&2
  exit 2
fi
if [[ ! -f signing.env ]]; then
  echo "error: signing.env not found, see CONTRIBUTING.md (CI and releases)" >&2
  exit 2
fi

# Before anything changes, so a failing build, lint or test never gets a version bump or tag
if ! ./scripts/check.sh -q; then
  echo "error: checks failed (see above), not releasing" >&2
  exit 3
fi

old_name=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' "$gradle_file")
old_code=$(sed -n 's/^ *versionCode = \([0-9]*\)$/\1/p' "$gradle_file")
if [[ $old_name == "$version" ]]; then
  # The tag doesn't exist (checked above), so this version was never released: release it as is
  echo "versionName is already $version, not bumping"
elif [[ $(printf '%s\n%s\n' "$old_name" "$version" | sort -V | tail -1) != "$version" ]]; then
  echo "error: $version is not newer than the current versionName $old_name" >&2
  exit 1
else
  new_code=$((old_code + 1))
  sed -i "s/^\( *versionName = \)\".*\"/\1\"$version\"/; s/^\( *versionCode = \)[0-9]*$/\1$new_code/" "$gradle_file"
  echo "versionName $old_name -> $version, versionCode $old_code -> $new_code"
fi

set -a; . ./signing.env; set +a
./gradlew -q assembleRelease
echo "signed release APK built: app/build/outputs/apk/release/app-release.apk"
