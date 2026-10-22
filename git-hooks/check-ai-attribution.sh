#!/bin/sh

msg_file="$1"

if grep -Eiq \
  '(^co-authored-by:.*\b(claude|copilot|cursor|codex|gemini)\b|generated with.*\b(claude|copilot|cursor|codex|gemini)\b|noreply@anthropic\.com|^co-authored-by:.*\[bot\])' \
  "$msg_file"
then
  echo "ERROR: AI attribution detected."
  echo "Remove AI co-author trailers or AI-generated notices."
  exit 1
fi
