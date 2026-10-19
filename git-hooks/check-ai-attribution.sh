#!/bin/sh

msg_file="$1"

if grep -Eiq \
  '(co-authored-by:.*(claude|copilot|cursor|codex)|generated with.*(claude|copilot|cursor|codex)|noreply@anthropic\.com|\[bot\])' \
  "$msg_file"
then
  echo "ERROR: AI attribution detected."
  echo "Remove AI co-author trailers or AI-generated notices."
  exit 1
fi
