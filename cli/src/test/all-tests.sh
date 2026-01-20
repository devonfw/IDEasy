#!/bin/bash
#set -e
#set -o pipefail

function doRestoreRcFiles() {
  # Restore shell RC files from backups to preserve user's existing configuration
  if [ -n "$BAK_BASHRC" ] && [ -f "$BAK_BASHRC" ]; then
    mv "$BAK_BASHRC" "$HOME/.bashrc"
    echo "Restored ~/.bashrc from backup"
  fi
  if [ -n "$BAK_ZSHRC" ] && [ -f "$BAK_ZSHRC" ]; then
    mv "$BAK_ZSHRC" "$HOME/.zshrc"
    echo "Restored ~/.zshrc from backup"
  fi
}

function doResetVariables() {
  IDE_HOME="${DEBUG_INTEGRATION_TEST}"
  export IDE_ROOT="${IDE_HOME}/projects"
  IDEASY_DIR="${IDE_ROOT}/_ide"
  IDEASY_INSTALLATION_DIR="${IDEASY_DIR}/installation"
  FUNCTIONS="${IDEASY_INSTALLATION_DIR}/functions"
  IDEASY_RELEASE_DIR_RELATIVE="software/maven/ideasy/ideasy/test"
  IDEASY_RELEASE_DIR="${IDEASY_DIR}/${IDEASY_RELEASE_DIR_RELATIVE}"
  IDE="${IDEASY_INSTALLATION_DIR}/bin/${BINARY_FILE_NAME}"
  TEST_RESULTS_FILE="${IDE_ROOT}/testResults"
}

function doTestsInner() {
  # Note: requires var test_files_directory to be set.
  for testpath in "${test_files_directory:?}/integration-tests"/*; do
    doResetVariables
    testcase="${testpath/*\//}"
    echo "Running test #${total}: ${testcase} (${testpath})"

    integration_test_result=0

    # Following line adds a trap to ERR signal: Whenever error (some
    # conditions apply; see documentation) set integration_test_result=1.
    trap 'doWarning "A non-handled error in integration test occurred."; integration_test_result=1' ERR

    source "${testpath:?}"

    # Remove trap
    # Disable errexit in case it was turned on in a test script
    trap - ERR
    set +e

    echo "RESULTS:"
    if [ "$integration_test_result" == 0 ]; then
      doSuccess "[SUCCESS] Succeeded running test #${total}: ${testcase}"
      ((success++))
      echo -e "\033[92m[SUCCESS] Succeeded running test #${total}: ${testcase}\033[39m" >> "${TEST_RESULTS_FILE:?}"
    else
      doError "[ERROR] Failed running test #${total}: ${testcase} - exit code ${integration_test_result}"
      ((failure++))
      echo -e "\033[91m[ERROR] Failed running test #${total}: ${testcase} - exit code ${integration_test_result}\033[39m" >> "${TEST_RESULTS_FILE:?}"
    fi
    ((total++))
  done
}

function doDisplayResults() {
  while read -r line; do echo -e "${line}"; done < "${TEST_RESULTS_FILE}"
}

function doTests () {
  doTestsInner
  echo -e "\n*****************************************************"
  echo "Executed #${total} test(s), #${success} succeeded and #${failure} failed"
  echo -e "*****************************************************\n"
  if [ "${failure}" == 0 ]; then
    doSuccess "All test succeeded. Fine!"
    doDisplayResults
  else
    doWarning "There are test failures! Please check the logs and fix errors.\n"
    doDisplayResults
    exit 1
  fi
  exit 0
}

# Workaround to create license.agreement file and simulate a proper installation.
mkdir -p "${HOME}"/.ide
touch "${HOME}"/.ide/.license.agreement

source "$(dirname "${0}")"/all-tests-functions.sh

# Remove side-effects
BAK_IDE_ROOT="${IDE_ROOT}"
BAK_PATH="${PATH}"
DEBUG_INTEGRATION_TEST="${HOME}/tmp/ideasy-integration-test-debug"
if [ -e "${DEBUG_INTEGRATION_TEST}" ]; then
  echo "Deleting previous test folder ${DEBUG_INTEGRATION_TEST}"
  rm -rf "${DEBUG_INTEGRATION_TEST}"
fi
# Create backups of shell RC files to prevent destroying user's existing configuration
BAK_BASHRC=""
BAK_ZSHRC=""
if [ -f "$HOME/.bashrc" ]; then
  BAK_BASHRC="$HOME/.bashrc.ideasy-test-backup"
  cp "$HOME/.bashrc" "$BAK_BASHRC"
fi
if [ -f "$HOME/.zshrc" ]; then
  BAK_ZSHRC="$HOME/.zshrc.ideasy-test-backup"
  cp "$HOME/.zshrc" "$BAK_ZSHRC"
fi

trap "export PATH=\"${BAK_PATH}\" && export IDE_ROOT=\"${BAK_IDE_ROOT}\" && doRestoreRcFiles && echo \"PATH, IDE_ROOT, and shell RC files restored\"" EXIT

# Switch IDEasy binary file name based on github workflow matrix.os name (first argument of all-tests.sh)
BINARY_FILE_NAME="ideasy"
if doIsWindows; then
  BINARY_FILE_NAME="ideasy.exe"
fi

doResetVariables
test_files_directory=$(realpath "$0" | xargs dirname)

success=0
failure=0
total=0

echo "Running integration tests from directory: ${test_files_directory}"

  # Only need to mkdir once:
  echo "Creating IDEasy directory at: ${IDEASY_DIR}"
  mkdir -p "${IDEASY_DIR}"

  echo "Switching directory to: ${IDEASY_DIR}"
  cd "${IDEASY_DIR}" || exit

# Determine IDEasy release to use for testing (default: downloads latest release)
# NOTE: For debugging purposes, if you want to avoid download time, you can
# uncomment var snapshot, set it to a local compressed IDEasy release
local_release=""
#snapshot="$HOME/tmp/downloads/ide-cli-2025.04.001-20250404.093145-4-linux-x64.tar.gz"
doDownloadRelease "${local_release}"
# Extract IDEasy and setup
mkdir -p "${IDEASY_RELEASE_DIR}"
echo "Extracting IDEasy archive ${IDEASY_COMPRESSED_FILE} to ${IDEASY_RELEASE_DIR}"
tar xfz "${IDEASY_COMPRESSED_FILE:?}" --directory "${IDEASY_RELEASE_DIR}" || exit 1
cd ${IDEASY_DIR}
doCreateLink ${IDEASY_RELEASE_DIR_RELATIVE} installation
# avoid cloning urls on every run in local execution
IDE_URLS="${BAK_IDE_ROOT}/_ide/urls"
if [ -d "${IDE_URLS}" ]; then
  doCreateLink "$IDE_URLS" urls
fi

echo "Switching directory to: ${IDE_ROOT}"
cd "${IDE_ROOT}" || exit 1


# upgrade to latest snapshot
echo "Upgrading IDEasy to latest SNAPSHOT"
$IDE -d --batch upgrade --mode=snapshot || exit 1 #echo "Upgrade failed, continuing with downloaded version"

# source functions (resets IDEasy)
echo "Sourcing functions from: ${FUNCTIONS}"
# Add IDE bin to PATH so ideasy command can be found
export PATH="${IDEASY_DIR}/installation/bin:$PATH"
# Try installation path first, then fall back to root
if [ -f "${FUNCTIONS:?}" ]; then
  source "${FUNCTIONS:?}"
elif [ -f "${IDEASY_DIR}/functions" ]; then
  echo "Using functions from root: ${IDEASY_DIR}/functions"
  source "${IDEASY_DIR}/functions"
else
  echo "ERROR: Could not find functions file"
  exit 1
fi

echo "Checking version after upgrade"
which ideasy
ide -v

doIdeCreate

doTests

echo "DONE"
exit 0

