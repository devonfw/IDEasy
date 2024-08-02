#!/bin/bash

cd "$(dirname "${BASH_SOURCE:-$0}")" || exit 255

if [ ! -d ~/Library/KeyBindings ]
then
  echo "This does not look like MacOS!"
  exit -1
fi

echo "installing KeyBindings..."
mkdir -p ~/Library/KeyBindings
cp ./KeyBindings/*.dict ~/Library/KeyBindings

echo "installing Keyboard Layout..."
mkdir -p ~/Library/"Keyboard Layouts"
cp ./"Keyboard Layouts"/*.bundle ~/Library/"Keyboard Layouts"

echo "Advanced keyboard support has been installed to your system."
echo "The changes will only affect after you reboot your MacOS system".
