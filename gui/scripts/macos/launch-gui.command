#!/bin/bash

# IDEasy GUI Launcher for macOS
# This script launches the IDEasy GUI using the native 'ide gui' command
# The GUI runs in the background, the script exits immediately

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    echo "Error: IDEasy project root not found at $PROJECT_ROOT"
    echo "Please ensure this script is located in: IDEasy/gui/scripts/macos/"
    read -p "Press Enter to close..."
    exit 1
fi

# 'ide' is a shell function defined in $IDE_ROOT/_ide/installation/functions and
# sourced by ~/.bashrc / ~/.zshrc — those only get sourced in interactive shells.
# Double-clicking a .command file runs it via its #!/bin/bash shebang directly,
# which is a non-interactive shell, so 'ide' is invisible there even though it
# works fine from a regular Terminal window. Route through an interactive bash
# so the function gets sourced regardless of how this script was launched.
if ! bash -ic "command -v ide" &> /dev/null; then
    echo ""
    echo "Error: IDEasy is not installed"
    echo ""
    echo "Please install IDEasy first:"
    echo "https://github.com/devonfw/IDEasy#setup"
    echo ""
    read -p "Press Enter to close..."
    exit 1
fi

cd "$PROJECT_ROOT"

# Launch IDE GUI in background and exit immediately
LOG_FILE="$HOME/.ideasy-gui.log"
bash -ic "ide gui" >> "$LOG_FILE" 2>&1 &
disown

exit 0
