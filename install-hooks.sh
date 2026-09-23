#!/bin/bash
# Installs the IDEasy git hooks for THIS clone.
#
# It points git's core.hooksPath at the version-controlled git-hooks/ directory.
# This is repo-local and fully isolated: it writes only to .git/config of this clone,
# never to your global git config, and cannot affect any other repository on your machine.
# When core.hooksPath is set, git ignores .git/hooks entirely (the generated *.sample files
# are left untouched).
#
# Run once per clone. On Windows use Git Bash (comes with Git for Windows):
#   ./install-hooks.sh
#
# Uninstall with:
#   git config --unset core.hooksPath

set -eu

green() { printf '\033[1;32m%s\033[0m\n' "$1"; }
blue()  { printf '\033[1;34m%s\033[0m\n' "$1"; }
red()   { printf '\033[1;31m%s\033[0m\n' "$1"; }
abort() { red "Error: $1" >&2; exit 1; }

command -v git >/dev/null 2>&1 || abort "git is required but not installed."

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || abort "Not inside a git repository."
cd "$REPO_ROOT"

HOOKS_DIR="git-hooks"
[ -d "$HOOKS_DIR" ] || abort "Missing '$HOOKS_DIR' directory in the repository root ($REPO_ROOT)."

blue "Installing IDEasy git hooks (repo-local, via core.hooksPath)..."

# Repo-local config only (no --global): affects just this clone.
git config core.hooksPath "$HOOKS_DIR"

# Ensure hook scripts are executable (relevant on macOS/Linux; harmless on Windows).
chmod +x "$HOOKS_DIR"/* 2>/dev/null || true

green "Done. core.hooksPath -> $(git config --local core.hooksPath)"
echo ""
echo "Active hooks:"
for h in "$HOOKS_DIR"/*; do
  case "$h" in
    *.md|*README*) : ;;                       # skip documentation
    *) [ -f "$h" ] && echo "  - $(basename "$h")" ;;
  esac
done
echo ""
echo "The pre-commit hook runs 'mvn verify' before each commit."
echo "  Skip once:             git commit --no-verify"
echo "  Also run format/lint:  IDEASY_HARNESS=1 git commit ..."
echo "  Uninstall:             git config --unset core.hooksPath"
