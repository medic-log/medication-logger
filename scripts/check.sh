#!/usr/bin/env bash
# The checks CI runs: debug build, lint and JVM unit tests. Used by CI, the pre-push hook and /check.
# On failure, prints the lint errors and failed tests so there's no need to open the HTML reports.
# Exit codes: 0 all passed, 1 something failed.
set -uo pipefail
cd "$(dirname "$0")/.."

# --continue so one failure (e.g. lint) doesn't hide another (e.g. a failing test);
# --quiet keeps the log to errors, which are summarized below
if ./gradlew assembleDebug lint testDebugUnitTest --continue --quiet "$@"; then
    exit 0
fi

lint=app/build/reports/lint-results-debug.txt
if [[ -f $lint ]] && grep -q ': Error:' "$lint"; then
    echo
    echo "== Lint errors (full report: app/build/reports/lint-results-debug.html)"
    grep ': Error:' "$lint"
fi

failed=$(grep -l '<failure' app/build/test-results/testDebugUnitTest/*.xml 2>/dev/null)
if [[ -n $failed ]]; then
    echo
    echo "== Failed tests (full report: app/build/reports/tests/testDebugUnitTest/index.html)"
    for f in $failed; do
        # One line per failing test case: class > name: message
        awk '/<testcase/ { match($0, /name="[^"]*"/); name = substr($0, RSTART + 6, RLENGTH - 7)
                           match($0, /classname="[^"]*"/); cls = substr($0, RSTART + 11, RLENGTH - 12) }
             /<failure/  { match($0, /message="[^"]*"/); print cls " > " name ": " substr($0, RSTART + 9, RLENGTH - 10) }' "$f" |
            sed 's/&lt;/</g; s/&gt;/>/g; s/&quot;/"/g; s/&#10;/ /g; s/&amp;/\&/g'
    done
fi
exit 1
