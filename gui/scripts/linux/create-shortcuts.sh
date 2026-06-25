#!/bin/bash

# IDEasy GUI Shortcut Creator for Linux
# Creates .desktop files for easy GUI launching from the Application Menu and Desktop
# Supports: GNOME, KDE, XFCE, and other freedesktop-compatible environments

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LAUNCHER_SCRIPT="$SCRIPT_DIR/launch-gui.sh"
ICON_PATH="$SCRIPT_DIR/../../src/main/resources/com/devonfw/ide/gui/assets/devonfw.png"

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

# Resolve icon: use bundled devonfw.png if available, otherwise fall back to system theme
if [ -f "$ICON_PATH" ]; then
    ICON="$(cd "$(dirname "$ICON_PATH")" && pwd)/$(basename "$ICON_PATH")"
else
    ICON="application-x-executable"
fi

# Create Desktop Entry (.desktop file)
function create_desktop_entry() {
    local target_dir="$1"
    local desktop_file="$target_dir/ideasy-gui.desktop"

    if ! mkdir -p "$target_dir" 2>/dev/null; then
        print_error "Failed to create directory: $target_dir"
        return 1
    fi

    if ! cat > "$desktop_file" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=IDEasy GUI
Comment=Launch IDEasy Integrated Development Environment GUI
Exec=$LAUNCHER_SCRIPT
Icon=$ICON
Terminal=false
Categories=Development;IDE;
StartupNotify=true
EOF
    then
        print_error "Failed to write: $desktop_file"
        return 1
    fi

    # Make it executable (required for some desktop environments)
    if ! chmod +x "$desktop_file" 2>/dev/null; then
        print_error "Failed to make executable: $desktop_file"
        return 1
    fi

    print_success "Created: $desktop_file"
    return 0
}

# Create application menu entry
APPLICATIONS_DIR="$HOME/.local/share/applications"
if ! create_desktop_entry "$APPLICATIONS_DIR"; then
    print_info "Note: Could not create application menu entry (may require additional permissions)"
else
    # Refresh the application menu database so the entry appears immediately
    update-desktop-database "$APPLICATIONS_DIR" 2>/dev/null || true
    print_info "Launch from: Application Menu or Launcher"
fi

# Determine desktop directory via XDG standard (respects custom Desktop locations).
# On systems where xdg-user-dirs was never configured (e.g. minimal WM setups),
# xdg-user-dir falls back to printing $HOME itself — guard against that so we
# don't drop a loose .desktop file directly into the user's home directory.
DESKTOP_DIR=$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")
DESKTOP_DIR="${DESKTOP_DIR%/}"
if [ -z "$DESKTOP_DIR" ] || [ "$DESKTOP_DIR" = "$HOME" ]; then
    DESKTOP_DIR="$HOME/Desktop"
fi
if [ -d "$DESKTOP_DIR" ]; then
    if ! create_desktop_entry "$DESKTOP_DIR"; then
        print_info "Note: Desktop entry creation requires write permissions"
    else
        # GNOME 3.28+: mark desktop file as trusted so it can be launched by double-click
        gio set "$DESKTOP_DIR/ideasy-gui.desktop" "metadata::trusted" true 2>/dev/null || true
        print_info "Launch from: Desktop"
    fi
else
    print_info "Desktop directory not found (Desktop feature may not be available)"
fi

echo ""
print_success "IDEasy GUI shortcuts ready!"
echo ""
echo "Usage:"
echo "  • Open your Application Menu and search for 'IDEasy GUI'"
echo "  • Or double-click the shortcut on your Desktop"
echo "  • First launch may take longer as Maven downloads dependencies"
echo ""
