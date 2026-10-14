#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
git add .
if ! git diff --cached --quiet; then
  git commit -m "Update Laux project"
fi
git push origin main
