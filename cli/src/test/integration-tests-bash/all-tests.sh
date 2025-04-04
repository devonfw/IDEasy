#!/bin/bash
#set -eu
set -o pipefail

source "$(dirname "${0}")"/functions-test.sh

START_TIME=$(date '+%Y-%m-%d_%H-%M-%S')

DEBUG_INTEGRATION_TEST_PREFIX="$HOME/tmp/ideasy-integration-test-debug"
DEBUG_INTEGRATION_TEST="${DEBUG_INTEGRATION_TEST_PREFIX}-${START_TIME}"
IDE_HOME="${DEBUG_INTEGRATION_TEST}/home-dir"
IDE_ROOT="${IDE_HOME}/projects"
IDEASY_DIR="${IDE_ROOT}/_ide"
FUNCTIONS="${IDEASY_DIR}/installation/functions"
IDE="${DEBUG_INTEGRATION_TEST}/home-dir/projects/_ide/bin/ide"
TEST_RESULTS_FILE="${IDE_ROOT}/testResults"

test_files_directory=$(realpath "$0" | xargs dirname)

success=0
failure=0
total=0

function doTestsInner() {
  # Note: requires var test_files_directory to be set.
  echo "Entering doTestsInner..."
  local test_files_prefix="integration-test"
  for testpath in "${test_files_directory:?}"/"${test_files_prefix}"-*; do
    testcase="${testpath/*\//}"
    echo "Running test #${total}: ${testcase} (${testpath})"
    # Every test runs in its own environment.
    # Need to return back from created environment (integration test might have changed pwd).
    cd "${IDE_ROOT:?}" || exit

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
    echo "$integration_test_result"
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

function main () {
  echo "Running integration tests from directory: ${test_files_directory}"

  # rm -rf "${DEBUG_INTEGRATION_TEST_PREFIX}"
  # Only need to mkdir once:
  echo "Creating IDEasy directory at: ${IDEASY_DIR}"
  mkdir -p "${IDEASY_DIR}"

  echo "Switching directory to: ${IDEASY_DIR}"
  cd "${IDEASY_DIR}" || exit

  # Will call IDEasy with variable
  #ide="${IDE_ROOT}/_ide/bin/ide"
  #alias ide="source ${IDE_ROOT}/_ide/bin/ide"
  # TODO REMOVE? We will simulate having a virtual home.
  #no need to: cd "${HOME}"
  #echo -e "echo \"Tmp home dir's bashrc loading...\"" >> ~/.bashrc
  #echo -e "alias ide=\"source ${PWD}/bin/ide\"" >> ~/.bashrc
  # echo -e 'export IDE_ROOT="$(pwd)"' >> ~/.bashrc
  #source ~/.bashrc

  # Determine IDEasy release to use for testing (default: downloads latest release)
  # NOTE: For debugging purposes, if you want to avoid download time, you can
  # uncomment var SNAPSHOT, set it to a local compressed IDEasy release and
  # give it to 'doDownloadSnapshot' as first argument..
  local SNAPSHOT=""
  #local SNAPSHOT="/c/Users/nmollers/Downloads/ide-cli-2024.10.001-beta-20241029.023922-8-windows-x64.tar.gz"
  doDownloadSnapshot "${SNAPSHOT}"
  # Extract IDEasy and setup
  doExtract

  # source ./bin/ide
  echo "Switching directory to: ${IDE_ROOT}"
  cd "${IDE_ROOT}" || exit

  # upgrade to latest snapshot
  echo "Upgrading IDEasy to latest SNAPSHOT"
  $IDE upgrade --mode=snapshot

  # source functions (resets IDEasy)
  echo "Sourcing functions to: ${FUNCTIONS}"
  source "${FUNCTIONS:?}"

  echo "Running 'ide -v'"
  $IDE -v

  doTests

  echo "DONE"
  exit 0
}

main

