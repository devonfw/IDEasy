#!/bin/bash

# IDEasy GUI Launcher for Linux
# This script launches the IDEasy GUI using the native 'ide gui' command
# The GUI runs in the background, the script exits immediately

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    echo "Error: IDEasy project root not found at $PROJECT_ROOT"
    echo "Please ensure this script is located in: IDEasy/gui/scripts/linux/"
    exit 1
fi

if ! command -v ide &> /dev/null; then
    echo ""
    echo "Error: IDEasy is not installed"
    echo ""
    echo "Please install IDEasy first:"
    echo "https://github.com/devonfw/IDEasy#setup"
    echo ""
    exit 1
fi

cd "$PROJECT_ROOT"

# Launch IDE GUI in background and exit immediately
# Redirect output to suppress any console messages
ide gui > /dev/null 2>&1 &

# Give the process a moment to start (may need time for Maven dependencies on first run)
sleep 3

exit 0
