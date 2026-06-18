#!/bin/bash

# IDEasy GUI Launcher for Linux
# This script launches the IDEasy GUI using the native 'ide gui' command
# The GUI runs in the background, the script exits immediately

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_FILE="$HOME/.ideasy-gui.log"

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

if ! command -v ide &> /dev/null; then
    echo "Error: IDEasy is not installed or not in PATH"
    echo "Please install IDEasy first: https://github.com/devonfw/IDEasy#setup"
    notify_error "'ide' command not found in PATH (see $LOG_FILE)"
    exit 1
fi

cd "$PROJECT_ROOT"

# Launch IDE GUI in background and exit immediately
ide gui &

exit 0
