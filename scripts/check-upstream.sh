#!/usr/bin/env bash
# Zeigt Änderungen im Original-Repo (Remote "upstream") seit der letzten Prüfung.
# Mit --mark wird der aktuelle Stand als geprüft gespeichert.
set -euo pipefail

cd "$(dirname "$0")/.."
git fetch upstream --quiet

marker="$(git rev-parse --git-dir)/upstream-reviewed"
if [[ -f "$marker" ]]; then
  base="$(cat "$marker")"
else
  base="$(git merge-base HEAD upstream/main)"
fi

echo "Geprüft bis: $base"
echo "=== Neue Commits im Original ==="
git log --format='%h %ad %s' --date=short "$base..upstream/main" || true
echo "=== Geänderte Dateien ==="
git diff --stat "$base" upstream/main || true

if [[ "${1:-}" == "--mark" ]]; then
  git rev-parse upstream/main > "$marker"
  echo "Stand als geprüft gespeichert."
fi
