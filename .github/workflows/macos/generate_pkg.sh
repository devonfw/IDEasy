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
mvn -B -ntp install # no clean here, this would delete our copy results from above (beginning line 7)!
# Determine architecture: macos-latest -> arm64, macos-15-intel -> x64
if [[ "$RUNNER_ARCH" == "ARM64" ]] || [[ "$(uname -m)" == "arm64" ]]; then
  PKG_ARCH="arm64"
else
  PKG_ARCH="x64"
fi
PKG_FILE="ideasy-${PKG_ARCH}.pkg"
# shellcheck disable=SC2154
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
# pkgbuild is responsible for building the installation bundle. By using productbuild, we can custimze the installation process, including things like showing a license.
if productbuild --distribution Distribution.xml \
    --resources Resources \
    --package-path . \
    "$PKG_FILE"; then
    echo "productbuild succeeded: $PKG_FILE"
else
    echo "productbuild failed with exit code $?" >&2
    exit 1
fi
# Copy to expected output location for upload
cp "$PKG_FILE" ../ideasy.pkg
