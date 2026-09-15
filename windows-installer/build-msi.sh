#!/usr/bin/env bash

set -euo pipefail

mvn -B -ntp -f documentation/pom.xml clean install

mkdir -p windows-installer/msi-files
cp documentation/target/generated-docs/IDEasy.pdf windows-installer/msi-files
cp -r cli/target/package/* windows-installer/msi-files
rm -rf windows-installer/msi-files/system/mac
rm -rf windows-installer/msi-files/system/linux
cp cli/target/ideasy.exe windows-installer/msi-files/bin

cd windows-installer

if ! command -v wix > /dev/null 2>&1; then
  dotnet tool install --global wix --version 5.0.2
fi
wix extension add WixToolset.UI.wixext/5.0.2
wix extension add WixToolset.Util.wixext/5.0.2

wix build \
  Package.wxs \
  WixUI_IDEasySetup.wxs \
  WixUI_FirstStepsDlg.wxs \
  -loc Package.en-us.wxl \
  -ext WixToolset.UI.wixext \
  -ext WixToolset.Util.wixext \
  -o ideasy.msi
