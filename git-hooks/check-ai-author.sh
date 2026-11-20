#!/bin/sh

msg_file="$1"

# ignore the diff that "git commit -v" appends below the scissors line
if sed '/^# -\{24\} >8 -\{24\}$/,$d' "$msg_file" | grep -Eiq \
  'author:.*(\b(claude|copilot|cursor|codex|gemini)\b|\[bot\]|noreply@anthropic\.com)'
then
  echo "ERROR: AI authorship detected."
  echo "Enter a valid GitHub-username for the author."
  exit 1
fi
