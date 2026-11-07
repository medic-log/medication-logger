#!/usr/bin/env bash
# Runs the web app locally and opens it in the browser, installing its dependencies first if they're missing.
#   scripts/web.sh            dev server with hot reload (http://localhost:5173/medication-logger/app/)
#   scripts/web.sh --preview  the production build, with its service worker, as deployed
#                             (http://localhost:4173/medication-logger/app/)
# Other arguments go to Vite, e.g. --host to open it from a phone on the same network.
set -euo pipefail
cd "$(dirname "$0")/../web"

[[ -d node_modules ]] || npm ci

if [[ "${1:-}" == "--preview" ]]; then
    shift
    npm run build
    exec npx vite preview --open "$@"
fi
exec npx vite --open "$@"
