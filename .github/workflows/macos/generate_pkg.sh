#!/usr/bin/env bash
# Fail on any error (a PKG built from an incomplete pkg-root would install a broken IDEasy),
# on unset variables and on failures in the middle of a pipe.
set -euo pipefail

# PKG_VERSION is provided as env by the "Build MacOS PKG" steps in release.yml (release version)
# and nightly-build.yml (snapshot version). Fail early instead of building a PKG without version.
PKG_VERSION="${PKG_VERSION:?PKG_VERSION must be set (see release.yml/nightly-build.yml)}"

cd documentation
mvn -B -ntp clean install
cd ..
echo "working dir: $PWD"
mkdir -p ./macos-installer/pkg-root/bin
mkdir -p ./macos-installer/Resources
cp documentation/target/generated-docs/IDEasy.pdf macos-installer/pkg-root/
cp documentation/target/generated-docs/LICENSE.rtf macos-installer/Resources/LICENSE.rtf
cp -r ./cli/target/package/* macos-installer/pkg-root/
cp ./cli/target/ideasy macos-installer/pkg-root/bin/
rm -rf ./macos-installer/pkg-root/system/windows
rm -rf ./macos-installer/pkg-root/system/linux
chmod +x macos-installer/pkg-root/bin/ideasy
chmod +x macos-installer/scripts/postinstall
cd macos-installer
# -Drevision keeps Distribution.xml (installer title, pkg-ref version) in sync with the
# pkgbuild --version below: without it Maven would resolve ${revision} from .mvn/maven.config,
# which during a release build still contains the SNAPSHOT version (release.yml only rewrites it
# later in the release job). No clean here, this would delete our copy results from above!
mvn -B -ntp install -Drevision="$PKG_VERSION"
# Determine architecture: macos-latest -> arm64, macos-15-intel -> x64
if [[ "${RUNNER_ARCH:-}" == "ARM64" ]] || [[ "$(uname -m)" == "arm64" ]]; then
  PKG_ARCH="arm64"
else
  PKG_ARCH="x64"
fi
PKG_FILE="ideasy-${PKG_ARCH}.pkg"
if pkgbuild --root pkg-root \
    --identifier com.devonfw.ideasy \
    --version "$PKG_VERSION" \
    --install-location /projects/_ide/tmp/ideasy \
    --scripts scripts \
    IDEasyComponent.pkg; then
    echo "pkgbuild succeeded"
else
    echo "pkgbuild failed with exit code $?" >&2
    exit 1
fi
# pkgbuild is responsible for building the installation bundle. By using productbuild, we can customize the installation process, including things like showing a license.
if productbuild --distribution Distribution.xml \
    --resources Resources \
    --package-path . \
    "$PKG_FILE"; then
    echo "productbuild succeeded: $PKG_FILE"
else
    echo "productbuild failed with exit code $?" >&2
    exit 1
fi
# Copy to the fixed name that the "Upload unsigned PKG" steps in release.yml and
# nightly-build.yml expect (macos-installer/ideasy.pkg - we are still inside macos-installer here).
cp "$PKG_FILE" ideasy.pkg
