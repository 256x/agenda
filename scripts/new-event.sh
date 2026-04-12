#!/bin/bash
# new-event.sh - create a new event from terminal
# Usage: ./new-event.sh
#
# Opens a pre-filled event file in $EDITOR (fallback: vim)

EVENTS="$(dirname "$0")/../events"
mkdir -p "$EVENTS"

# Prompt for date (default: today)
read -rp "Date (YYYY-MM-DD) [$(date +%Y-%m-%d)]: " date
date="${date:-$(date +%Y-%m-%d)}"

# Prompt for time (optional)
read -rp "Time (HH:MM) [leave blank for all-day]: " time

# Prompt for repeat
read -rp "Repeat (none/weekly/monthly/yearly) [none]: " repeat
repeat="${repeat:-none}"

filename="$(date -u +%Y%m%d_%H%M%S).md"
filepath="$EVENTS/$filename"

{
  echo "---"
  echo "date: $date"
  [ -n "$time" ] && echo "time: $time"
  echo "repeat: $repeat"
  echo "---"
  echo ""
} > "$filepath"

${EDITOR:-vim} "$filepath"
