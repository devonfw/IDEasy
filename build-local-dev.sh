#!/usr/bin/env bash
set -euo pipefail

if [ -z "${IDE_ROOT:-}" ]; then
  echo "Error: IDE_ROOT is not set."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

CLI_DIR="$SCRIPT_DIR/cli"
TARGET_DIR="$CLI_DIR/target"
PACKAGE_DIR="$CLI_DIR/src/main/package"

PROJECT_SOFTWARE_DIR="$PROJECT_DIR/software"
GRAALVM_DIR="$PROJECT_SOFTWARE_DIR/extra/graalvm"

LOCAL_DEV="$IDE_ROOT/_ide/software/maven/ideasy/ideasy/local-dev"
INSTALLATION_LINK="$IDE_ROOT/_ide/installation"

echo "Building IDEasy native image..."

if [ ! -d "$GRAALVM_DIR" ]; then
  echo "Error: GraalVM is not installed for this IDEasy project:"
  echo "$GRAALVM_DIR"
  echo
  echo "Please install GraalVM first:"
  echo "ide install graalvm"
  exit 1
fi

export PATH="$GRAALVM_DIR/bin:$PATH"

cd "$CLI_DIR"
mvn -B -ntp -Pnative -DskipTests=true package

echo "Preparing local-dev installation..."
rm -rf "$LOCAL_DEV"
mkdir -p "$LOCAL_DEV"

echo "Copying package contents..."

if [ ! -d "$PACKAGE_DIR" ]; then
  echo "Error: Package directory not found: $PACKAGE_DIR"
  exit 1
fi

cp -R "$PACKAGE_DIR"/. "$LOCAL_DEV"/

OS_NAME="$(uname -s)"

echo "Creating local-dev software version marker..."
echo "local-dev-version" > "$LOCAL_DEV/.ide.software.version"

mkdir -p "$LOCAL_DEV/bin"

echo "Copying IDEasy executable and native libraries..."

if [ -f "$TARGET_DIR/ideasy.exe" ]; then
  cp "$TARGET_DIR/ideasy.exe" "$LOCAL_DEV/bin/ideasy.exe"
fi

if [ -f "$TARGET_DIR/ideasy" ]; then
  cp "$TARGET_DIR/ideasy" "$LOCAL_DEV/bin/ideasy"
  chmod +x "$LOCAL_DEV/bin/ideasy"
fi

if [ ! -f "$LOCAL_DEV/bin/ideasy.exe" ] && [ ! -f "$LOCAL_DEV/bin/ideasy" ]; then
  echo "Error: No ideasy executable found in $TARGET_DIR"
  exit 1
fi

if [ -f "$LOCAL_DEV/functions" ]; then
  chmod +x "$LOCAL_DEV/functions"
fi

if [ -f "$LOCAL_DEV/setup" ]; then
  chmod +x "$LOCAL_DEV/setup"
fi

echo "Updating IDEasy installation link..."

if ! command -v ideasy > /dev/null 2>&1; then
  echo "Error: ideasy command not found."
  exit 1
fi

IDEASY_CMD="$(readlink -f "$(command -v ideasy)")"

if [ -L "$INSTALLATION_LINK" ]; then
  unlink "$INSTALLATION_LINK"
elif [ -e "$INSTALLATION_LINK" ]; then
  echo "Error: $INSTALLATION_LINK exists but is not a symbolic link."
  echo "Aborting to avoid deleting a real folder."
  exit 1
fi

"$IDEASY_CMD" ln -s "$LOCAL_DEV" "$INSTALLATION_LINK"

echo "Done."
echo "You can test it with:"
echo "ide ..."
echo
echo "To switch back to the latest stable IDEasy version, run:"
echo "ideasy upgrade --mode=stable"
echo
echo "To switch to the latest snapshot IDEasy version, run:"
echo "ideasy upgrade --mode=snapshot"
