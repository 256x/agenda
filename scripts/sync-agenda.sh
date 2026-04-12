#!/bin/bash
# sync-agenda.sh - sync events with remote repository
# Usage: ./sync-agenda.sh

REPO="$(dirname "$0")/.."
cd "$REPO" || exit 1

git pull

if ! git diff --quiet events/ repeating/ || git ls-files --others --exclude-standard events/ repeating/ | grep -q .; then
  git add events/ repeating/
  git commit -m "sync: $(date -u +%Y%m%d_%H%M%S)"
  git push
else
  echo "Nothing to sync."
fi
