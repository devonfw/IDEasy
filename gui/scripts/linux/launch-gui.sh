#!/bin/bash

# IDEasy GUI Launcher for Linux
# This script launches the IDEasy GUI using the native 'ide gui' command
# The GUI runs in the background, the script exits immediately

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_FILE="$HOME/.ideasy-gui.log"

# Ignore SIGHUP so this script (and what it launches) survives even if the
# process that handed off execution (desktop session bus, gio launch, etc.)
# disconnects before we are done.
trap '' HUP

# When launched from an application menu (Terminal=false), there is no console to
# show errors on. Redirect everything to a log file from the start so failures
# (e.g. 'ide' missing from the launcher's PATH) are diagnosable after the fact
exec >> "$LOG_FILE" 2>&1
echo "---- $(date) ----"

function notify_error() {
    command -v notify-send &> /dev/null && notify-send -i dialog-error "IDEasy GUI" "$1"
}

if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    echo "Error: IDEasy project root not found at $PROJECT_ROOT"
    echo "Please ensure this script is located in: IDEasy/gui/scripts/linux/"
    notify_error "Project root not found at $PROJECT_ROOT"
    exit 1
fi

# 'ide' is a shell function defined in $IDE_ROOT/_ide/installation/functions and
# sourced by ~/.bashrc / ~/.zshrc — those only get sourced in interactive shells.
# Application launchers (Terminal=false) start a plain, non-interactive shell, so
# 'ide' is invisible there even though it works fine from a terminal. Route through
# an interactive bash so the function gets sourced regardless of launch context.
if ! bash -ic "command -v ide" &> /dev/null; then
    echo "Error: IDEasy is not installed or not in PATH"
    echo "Please install IDEasy first: https://github.com/devonfw/IDEasy#setup"
    notify_error "'ide' command not found (see $LOG_FILE)"
    exit 1
fi

cd "$PROJECT_ROOT"

# Launch IDE GUI in background and exit immediately
bash -ic "ide gui" </dev/null &
disown

exit 0
