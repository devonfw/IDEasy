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
  cd "${IDE_ROOT}/${TEST_PROJECT_NAME}" || exit 1
}

function doIdeCreateCleanup () {
  rm -rf "${IDE_ROOT:?}/${TEST_PROJECT_NAME}"
}

function doDownloadRelease () {
  mkdir -p "$WORK_DIR_INTEG_TEST"
  if [ "$1" != "" ]; then
    if [ -f "$1" ] && [[ $1 == *.tar.gz ]]; then
      echo "Local release given. Copying to directory: ${WORK_DIR_INTEG_TEST}"
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
    osType=$(doGetOsType)
    architecture=$(doGetArchNameForOs "$osType")
    url=$(grep "href=\"https://.*${osType}-${architecture}.tar.gz" "$pageHtmlLocal" | grep -o "https://[^\"]*${osType}-${architecture}.tar.gz" | head -1)
    echo "Trying to download IDEasy for OS: ${osType} from: ${url} to: ${IDEASY_COMPRESSED_FILE:?} ..."
    curl -o "${IDEASY_COMPRESSED_FILE:?}" "$url"
    rm "${pageHtmlLocal:?}"
  fi
}

function doGetOsType() {
  local osType
  case "$OSTYPE" in
    msys*|cygwin*|win32*)
      osType="windows"
      ;;
    linux*|gnu*)
      osType="linux"
      ;;
    darwin*|macos*)
      osType="mac"
      ;;
    *)
      echo "Unknown OSTYPE: $OSTYPE. Falling back to windows (most common developer machine)." >&2
      osType="windows"
      ;;
  esac
  echo "Detected OS type: ${osType}" >&2
  echo "$osType"
}

doGetArchNameForOs() {
  local machine archEnv archWow
  local osType="$1"

  case "$osType" in
    linux|mac)
      machine="$(uname -m 2>/dev/null || echo "")"
      case "$machine" in
        arm64|aarch64)
          echo "arm64"
          ;;
        x86_64|amd64)
          echo "x64"
          ;;
        *)
          echo "Unknown architecture from uname -m: '$machine' (osType=$osType). Falling back to x64." >&2
          echo "x64"
          ;;
      esac
      ;;

    windows)
      # Prefer Windows-native environment variables (more reliable than uname in Git Bash/MSYS/Cygwin).
      # Normalize to lower-case for comparisons.
      archEnv="$(printf '%s' "${PROCESSOR_ARCHITECTURE:-}" | tr '[:upper:]' '[:lower:]')"
      archWow="$(printf '%s' "${PROCESSOR_ARCHITEW6432:-}" | tr '[:upper:]' '[:lower:]')"

      # If running as 32-bit on 64-bit Windows, PROCESSOR_ARCHITEW6432 may reveal the underlying OS arch.
      case "${archWow:-$archEnv}" in
        arm64)
          echo "arm64"
          ;;
        amd64|x86_64)
          echo "x64"
          ;;
        *)
          # Best-effort fallback for environments where env vars aren't available/accurate.
          machine="$(uname -m 2>/dev/null || echo "")"
          case "$machine" in
            arm64|aarch64)
              echo "arm64"
              ;;
            x86_64|amd64)
              echo "x64"
              ;;
            *)
              echo "Unknown Windows architecture (PROCESSOR_ARCHITECTURE='${archEnv}', PROCESSOR_ARCHITEW6432='${archWow}', uname -m='${machine}'). Falling back to x64." >&2
              echo "x64"
              ;;
          esac
          ;;
      esac
      ;;

    *)
      echo "Unsupported osType: $osType. Falling back to x64." >&2
      echo "x64"
      ;;
  esac
}


# doCreateLink <source> <target-link>
function doCreateLink() {
  echo "creating link from $1 to $2 in $PWD"
  if [ ! -e "$1" ]; then
    echo "Source file to link does not exist!"
    exit 1
  fi
  if doIsWindows; then
    cmd //c "mklink /J $(cygpath -w $2) $(cygpath -w $1)" || exit 1
  else
    ln -s "$1" "$2"
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
