#!/bin/bash
cd "$(dirname "$0")"
echo $PWD
echo "intellij linux $*"
echo "intellij linux $*" > intellijtest

if [ "${1}" == "installPlugins" ]; then
  echo "installed plugin: PluginNode{id=${@:2}}"
fi

if [ -n "${3}" ]; then
  echo "custom plugin repo url is: ${3}" > customRepoTest
fi
