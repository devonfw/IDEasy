#!/bin/bash

# IDEasy GUI Shortcut Creator for macOS
# Creates shortcuts for easy GUI launching from Finder
# Supports creating: Applications folder alias and Desktop alias

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LAUNCHER_SCRIPT="$SCRIPT_DIR/launch-gui.command"

# Set error handling - exit on error but allow catching specific errors
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function print_error() {
    echo -e "${RED}Error: $1${NC}" >&2
}

function print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

function print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Verify launcher script exists and is executable
if [ ! -f "$LAUNCHER_SCRIPT" ]; then
    print_error "Launcher script not found: $LAUNCHER_SCRIPT"
    exit 1
fi

chmod +x "$LAUNCHER_SCRIPT"

# Create an alias/shortcut using osascript (AppleScript)
function create_alias() {
    local source="$1"
    local target_dir="$2"
    local target_name="$3"
    
    if [ ! -f "$source" ]; then
        print_error "Source file not found: $source"
        return 1
    fi
    
    if ! mkdir -p "$target_dir" 2>/dev/null; then
        print_error "Failed to create directory: $target_dir"
        return 1
    fi
    
    # Properly escape paths for AppleScript
    local escaped_source="$(printf '%s\n' "$source" | sed 's/\\/\\\\/g; s/"/\\"/g')"
    local escaped_target_dir="$(printf '%s\n' "$target_dir" | sed 's/\\/\\\\/g; s/"/\\"/g')"
    local escaped_target_name="$(printf '%s\n' "$target_name" | sed 's/\\/\\\\/g; s/"/\\"/g')"
    
    local output
    output=$(osascript 2>&1 <<EOF
tell application "Finder"
    try
        make alias file to POSIX file "$escaped_source" at folder "$escaped_target_dir" with properties {name:"$escaped_target_name"}
        return "success"
    on error errMsg
        error errMsg
    end try
end tell
EOF
    )
    
    local exit_code=$?
    if [ $exit_code -eq 0 ] && [ "$output" = "success" ]; then
        print_success "Created shortcut: $target_dir/$target_name"
        return 0
    else
        print_error "Failed to create shortcut: $target_dir/$target_name"
        [ -n "$output" ] && print_error "Reason: $output"
        return 1
    fi
}

# Create Applications alias
APPS_DIR="$HOME/Applications"
if ! create_alias "$LAUNCHER_SCRIPT" "$APPS_DIR" "IDEasy GUI"; then
    print_info "Note: Could not create Applications shortcut (may require additional permissions)"
else
    print_info "Launch from: Applications > IDEasy GUI"
fi

# Create Desktop alias
DESKTOP_DIR="$HOME/Desktop"
if create_alias "$LAUNCHER_SCRIPT" "$DESKTOP_DIR" "IDEasy GUI"; then
    print_info "Launch from: Desktop"
else
    print_info "Note: Desktop shortcut creation requires manual steps"
    print_info "You can manually create a shortcut by:"
    print_info "1. Open Finder"
    print_info "2. Go to $SCRIPT_DIR"
    print_info "3. Right-click 'launch-gui.command' → Make Alias"
    print_info "4. Drag alias to Desktop or Applications"
fi

echo ""
print_success "IDEasy GUI shortcuts ready!"
echo ""
echo "Usage:"
echo "  • Open Finder and go to Applications"
echo "  • Double-click 'IDEasy GUI' to launch"
echo "  • Or double-click the Desktop shortcut"
echo ""
