#!/bin/sh

msg_file="$1"

# ignore the diff that "git commit -v" appends below the scissors line
if sed '/^# -\{24\} >8 -\{24\}$/,$d' "$msg_file" | grep -Eiq \
  '(^co-authored-by:.*\b(claude|copilot|cursor|codex|gemini)\b|generated with.*\b(claude|copilot|cursor|codex|gemini)\b|noreply@anthropic\.com|^co-authored-by:.*\[bot\])'
then
  echo "ERROR: AI attribution detected."
  echo "Remove AI co-author trailers or AI-generated notices."
  exit 1
fi
