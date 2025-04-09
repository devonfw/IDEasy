#!/bin/bash
cd "$(dirname "$0")"
echo $PWD
echo "pycharm linux $*"
echo "pycharm linux $*" > pycharmtest

if [ "${1}" == "installPlugins" ]; then
  echo "installed plugin: PluginNode{id=${@:2}}"
fi

if [ -n "${3}" ]; then
  echo "custom plugin repo url is: ${3}" > customRepoTest
fi
