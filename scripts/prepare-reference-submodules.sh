#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${TMPDIR:-$HOME}/levilaunchroid-source-for-submodules"
if ! command -v git >/dev/null 2>&1; then
  echo "ERRO: instale git primeiro: pkg install git -y" >&2
  exit 1
fi
if [ -d "$WORK/.git" ]; then
  git -C "$WORK" pull --ff-only
  git -C "$WORK" submodule update --init --recursive
else
  rm -rf "$WORK"
  git clone --recurse-submodules https://github.com/LiteLDev/LeviLaunchroid.git "$WORK"
fi
for path in app/src/main/cpp/preloader app/src/main/cpp/libHttpClient; do
  if [ ! -d "$WORK/$path" ] || [ -z "$(find "$WORK/$path" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
    echo "ERRO: submódulo vazio: $WORK/$path" >&2
    exit 1
  fi
  mkdir -p "$ROOT/$path"
  rm -rf "$ROOT/$path"
  cp -a "$WORK/$path" "$ROOT/$path"
done
echo "Submódulos nativos copiados. Próximo passo: ./gradlew assembleDebug"
