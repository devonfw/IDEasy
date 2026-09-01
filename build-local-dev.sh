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

PROJECT_SOFTWARE_DIR="$PROJECT_DIR/software"
GRAALVM_DIR="$PROJECT_SOFTWARE_DIR/extra/graalvm"

LOCAL_DEV="$IDE_ROOT/_ide/software/maven/ideasy/ideasy/local-dev"
INSTALLATION_LINK="$IDE_ROOT/_ide/installation"
LOCAL_DEV_REPO="$LOCAL_DEV/.m2"
LAUNCHER_POM="$TARGET_DIR/package/gui/pom.xml"

# Determine the version to stamp into the local-dev binary. It is derived from the current revision in .mvn/maven.config
# (e.g. 2026.08.002-SNAPSHOT): the -SNAPSHOT suffix is replaced with -DEV-BUILD so the running binary self-identifies as a
# local-dev installation (see IdeVersion#isLocalDevBuild). Passing -Drevision overrides the value from .mvn/maven.config,
# exactly like the release CI does (see .github/workflows/macos/generate_pkg.sh).
MAVEN_CONFIG="$SCRIPT_DIR/.mvn/maven.config"
SNAPSHOT_REVISION="$(sed -n 's/^-Drevision=//p' "$MAVEN_CONFIG")"
if [ -z "$SNAPSHOT_REVISION" ]; then
  echo "Error: could not determine the revision from $MAVEN_CONFIG."
  exit 1
fi
DEV_REVISION="${SNAPSHOT_REVISION%-SNAPSHOT}-DEV-BUILD"

echo "Building IDEasy native image with version $DEV_REVISION..."

if [ ! -d "$GRAALVM_DIR" ]; then
  echo "Error: GraalVM is not installed for this IDEasy project:"
  echo "$GRAALVM_DIR"
  echo
  echo "Please install GraalVM first:"
  echo "ide install graalvm"
  exit 1
fi

export PATH="$GRAALVM_DIR/bin:$PATH"

mvn -B -ntp -f "$CLI_DIR/pom.xml" -Pnative -DskipTests=true -Drevision="$DEV_REVISION" clean install

echo "Preparing local-dev installation..."
rm -rf "$LOCAL_DEV"
mkdir -p "$LOCAL_DEV"

echo "Copying package contents..."

if [ ! -d "$TARGET_DIR/package" ]; then
  echo "Error: Filtered package output not found: $TARGET_DIR/package"
  echo "This should be produced by the maven package phase above."
  exit 1
fi

cp -R "$TARGET_DIR/package"/. "$LOCAL_DEV"/

# Use the same -DEV-BUILD revision as the native build so ide-gui/ide-cli are installed into the self-contained repository with
# the same version the launcher POM (also stamped with -DEV-BUILD) requests. Without this the launcher would request
# ide-gui:<base>-DEV-BUILD while the repository only contains ide-gui:<base>-SNAPSHOT, making 'ide gui' fail offline resolution.
echo "Building the GUI into the self-contained maven repository with version $DEV_REVISION..."
mvn -B -ntp -f "$SCRIPT_DIR/pom.xml" -pl gui -am -DskipTests=true -Drevision="$DEV_REVISION" -Dmaven.repo.local="$LOCAL_DEV_REPO" install

echo "Seeding the GUI launcher (exec) maven plugin into the self-contained maven repository..."
mvn -B -ntp -f "$LAUNCHER_POM" org.codehaus.mojo:exec-maven-plugin:3.1.0:exec \
  -Dexec.executable=echo -Dexec.args=seeded \
  -Dmaven.repo.local="$LOCAL_DEV_REPO"

OS_NAME="$(uname -s)"

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
