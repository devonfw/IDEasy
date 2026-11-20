#!/bin/sh

msg_file="$1"

# ignore the diff that "git commit -v" appends below the scissors line
if sed '/^# -\{24\} >8 -\{24\}$/,$d' "$msg_file" | grep -Eiq \
  'author:.*(\b(claude|copilot|cursor|codex|gemini)\b|\[bot\]|noreply@anthropic\.com)'
then
  echo "ERROR: AI authorship detected."
  echo "Configure a human GitHub account:"
  echo "  git config user.name \"Your Name\""
  echo "  git config user.email \"your-email@example.com\""
  echo "Then create the commit again."
  exit 1
  fi
if sed '/^# -\{24\} >8 -\{24\}$/,$d' "$msg_file" | grep -Eiq \
  'author:.*(noreply@github.com)'
then
  echo "ERROR:Invalid username."
  echo "Configure a human GitHub account:"
    echo "  git config user.name \"Your Name\""
    echo "  git config user.email \"your-email@example.com\""
    echo "Then create the commit again."
    exit 1
fi
