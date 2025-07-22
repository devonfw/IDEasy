#!/bin/bash
#set -e
#set -o pipefail

WORK_DIR_INTEG_TEST="${HOME}/tmp/ideasy-integration-test-debug/IDEasy_snapshot"
IDEASY_COMPRESSED_NAME="ideasy_latest.tar.gz"
IDEASY_COMPRESSED_FILE="${WORK_DIR_INTEG_TEST}/${IDEASY_COMPRESSED_NAME}"
TEST_PROJECT_NAME="tmp-integ-test"

function doIdeCreate () {
  #TODO: determine the name of the currently executed script
  # If first argument is given, then it is the url for the ide create command (default is '-').
  local settings_url=${1:--}
  echo "Running ide --batch create ${TEST_PROJECT_NAME} ${settings_url}"
  ide --batch -d create "${TEST_PROJECT_NAME}" "${settings_url}"

  echo "Switching to directory: ${IDE_ROOT}/${TEST_PROJECT_NAME}"
  cd "${IDE_ROOT}/${TEST_PROJECT_NAME}" || exit
}

function doIdeCreateCleanup () {
  rm -rf "${IDE_ROOT:?}/${TEST_PROJECT_NAME}"
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
    local urlIdeasyLatest="https://github.com/devonfw/IDEasy/releases/latest"
    echo "Trying to download latest IDEasy release from ${urlIdeasyLatest} ..."
    local pageHtmlLocal="${WORK_DIR_INTEG_TEST}/integ_test_gh_latest.html"

    curl -L "$urlIdeasyLatest" > "$pageHtmlLocal"
    # TODO: A bit of a workaround. But works for the time being...
    # Note: Explanation for cryptic argument "\"" of 'cut': delimiting char after url link from href is char '"'
    local url
    # Change OS type based on github workflow matrix.os name
    local osType
    echo "DEBUG: MATRIX_OS='${MATRIX_OS}'"
    if [ "${MATRIX_OS}" == "windows-latest" ]; then
      osType="windows-x64"
    elif [ "${MATRIX_OS}" == "ubuntu-latest" ]; then
      osType="linux-x64"
    elif [ "${MATRIX_OS}" == "macos-latest" ]; then
      osType="mac-arm64"
    elif [ "${MATRIX_OS}" == "macos-13" ]; then
      osType="mac-x64"
    else
      # Default to linux if MATRIX_OS is not set
      osType="linux-x64"
    fi
    # Fallback in case this script is executed aside of the github workflow
    if [ -z "$osType" ]; then
      case "$OSTYPE" in
        msys*|cygwin*|win32*)
          osType="windows-x64"
          ;;
        linux*|gnu*)
          osType="linux-x64"
          ;;
        darwin*|macos*)
          # Try to distinguish between Apple Silicon and Intel Macs
          if sysctl -n machdep.cpu.brand_string 2>/dev/null | grep -qi "Apple"; then
            osType="mac-arm64"
          else
            osType="mac-x64"
          fi
          ;;
        *)
          echo "Unknown OSTYPE: $OSTYPE"
          exit 1
          ;;
      esac
    fi
    url=$(grep "href=\"https://.*${osType}.tar.gz" "$pageHtmlLocal" | grep -o "https://.*${osType}.tar.gz" | cut -f1 -d"\"")
    echo "Trying to download IDEasy for OS: ${osType} from: ${url} to: ${IDEASY_COMPRESSED_FILE:?} ..."
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

# $@: messages to input
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

# supports assertion types like contains and equals
# TODO: add more assertion types
assertThat() {
  local input="$1"
  local assertionType="$2"
  local expected="$3"

  case "$assertionType" in
    contains)
      if echo "$input" | grep -q -e "$expected"; then
        echo "Assertion passed: '$expected' found in input"
        return 0
      else
        echo "Assertion failed: '$expected' not found in input"
        return 1
      fi
      ;;
    equals)
      if [[ "$input" == "$expected" ]]; then
        echo "Assertion passed: input equals '$expected'"
        return 0
      else
        echo "Assertion failed: input does not equal '$expected'"
        return 1
      fi
      ;;
    exists)
      if [[ -f "$input" ]]; then
        echo "Assertion passed: file '$input' exists"
        return 0
      else
        echo "Assertion failed: file '$input' does not exist"
        return 1
      fi
      ;;
    *)
      echo "Unknown assertion type: '$assertionType'"
      return 2
      ;;
  esac
}
