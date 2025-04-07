#!/bin/bash
#set -eu
set -o pipefail

WORK_DIR_INTEG_TEST="${HOME}/tmp/ideasy-integration-test-debug/IDEasy_snapshot"
IDEASY_COMPRESSED_NAME="ideasy_latest.tar.gz"
IDEASY_COMPRESSED_FILE="${WORK_DIR_INTEG_TEST}/${IDEASY_COMPRESSED_NAME}"
test_project_name="tmp-integ-test"

function doIdeCreate () {
  #TODO: determine the name of the currently executed script
  # If first argument is given, then it is the url for the ide create command (default is '-').
  local settings_url=${1:--}
  echo "Running ide --batch create ${test_project_name} ${settings_url}"
  $IDE_INSTALLATION --batch -d create "${test_project_name}" "${settings_url}"

  echo "Switching to directory: ${IDE_ROOT}/${test_project_name}"
  cd "${IDE_ROOT}/${test_project_name}" || exit
}

function doIdeCreateCleanup () {
  rm -rf "${IDE_ROOT:?}/${test_project_name}"
}

function doDownloadSnapshot () {
  mkdir -p "$WORK_DIR_INTEG_TEST"
  if [ "$1" != "" ]; then
    if [ -f "$1" ] && [[ $1 == *.tar.gz ]]; then
      echo "Local snapshot given. Copying to directory: ${WORK_DIR_INTEG_TEST}"
      cp "$1" "$IDEASY_COMPRESSED_FILE"
    else
      echo "Expected a file ending with tar.gz - Given: ${1}"
      exit 1
    fi
  else
    echo "Trying to download latest IDEasy release..."
    local urlIdeasyLatest="https://github.com/devonfw/IDEasy/releases/latest"
    local pageHtmlLocal="${WORK_DIR_INTEG_TEST}/integ_test_gh_latest.html"

    curl -L "$urlIdeasyLatest" > "$pageHtmlLocal"
    # TODO: A bit of a workaround. But works for the time being...
    # Note: Explanation for cryptic argument "\"" of 'cut': delimiting char after url link from href is char '"'
    local url
    # Change OS type based on github workflow matrix.os name
    local osType
    if [ "${MATRIX_OS}" == "windows-latest" ]; then
      osType="windows-x64"
    elif [ "${MATRIX_OS}" == "ubuntu-latest" ]; then
      osType="linux-x64"
    elif [ "${MATRIX_OS}" == "macos-latest" ]; then
      osType="mac-arm"
    elif [ "${MATRIX_OS}" == "macos-13" ]; then
      osType="mac-x64"
    fi
    url=$(grep "href=\"https://.*${osType}.tar.gz" "$pageHtmlLocal" | grep -o "https://.*${osType}.tar.gz" | cut -f1 -d"\"")
    curl -o "${IDEASY_COMPRESSED_FILE:?}" "$url"
    rm "${pageHtmlLocal:?}"
  fi
}


function doExtract() {
  echo "Extracting IDEasy archive: ${IDEASY_COMPRESSED_FILE} to: ${IDEASY_DIR}"
  if [ -f "${IDEASY_COMPRESSED_FILE:?}" ]; then
    tar xfz "${IDEASY_COMPRESSED_FILE:?}" --directory "${IDEASY_DIR:?}" || exit 1
  else
    echo "Could not find and extract release ${IDEASY_COMPRESSED_FILE:?}"
    exit 1
  fi
}

# $@: success message
function doSuccess() {
  echo -e "\033[92m${*}\033[39m"
}

# $@: warning message
function doWarning() {
  echo -e "\033[93m${*}\033[39m"
}

# $@: messages to output
function doError() {
  echo -e "\033[91m${1}\033[39m"
}

function doIsMacOs() {
  if [ "${OSTYPE:0:6}" = "darwin" ]
  then
    return
  fi
  return 255
}

function doIsWindows() {
  if [ "${OSTYPE}" = "cygwin" ] || [ "${OSTYPE}" = "msys" ]
  then
    return
  fi
  return 255
}
