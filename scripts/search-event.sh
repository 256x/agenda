#!/bin/bash
# search-event.sh - search events with fzf
# Usage: ./search-event.sh [query]
#
# Requires: fzf
# Opens the selected event in $EDITOR (fallback: vim)

EVENTS="$(dirname "$0")/../events"
cd "$EVENTS" || exit 1

preview='echo "---"; grep -v "^---$" {} | grep -v "^repeat:" | sed "s/^date: /Date: /;s/^time: /Time: /"; echo "---"'

if [ -n "$1" ]; then
  selected=$(grep -rl "$1" . | sed 's|^\./||' | fzf --query="$1" --preview "$preview")
else
  selected=$(ls -t ./*.md 2>/dev/null | sed 's|^\./||' | fzf --preview "$preview" --preview-window=right:40%)
fi

[ -n "$selected" ] && ${EDITOR:-vim} "$selected"
