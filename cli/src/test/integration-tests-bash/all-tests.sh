#!/bin/bash

source "$(dirname "${0}")"/functions-test.sh

success=0
failure=0
total=0

function doTestsInner() {
    # Note: requires var test_files_directory to be set.
    echo "Enter doTestsInner..."
  echo $PWD
  echo "test_files_directory ---> ${test_files_directory}"/"${1}"-*
  for testpath in "${test_files_directory}"/"${1}"-*
  do
    testcase="${testpath/*\//}"
    echo "Running test #${total}: ${testcase}"
    result=1
    if [[ $testcase == integration* ]]
    then
      echo "Will run test script: $testcase"
      "${testpath}"
      result=${?}
#      cd test-setup
#      rm -fR "${PWD}/software"
#      rm -f "${PWD}/conf/devon.properties"
#      rm -f "${PWD}/settings/devon.properties"
#      echo "JAVA_VERSION=17*" >> "${PWD}/settings/devon.properties"
    else
      echo "TODO -- for now ignore non-integration tests: $testcase".
      echo "is this code reachable in the first place?"
  #    mkdir -p "${testcase}/conf"
  #    cd "${testcase}"
  #    echo "export M2_REPO=~/.m2/repository" > "conf/devon.properties"
    fi
    if [ "${result}" == 0 ]
    then
      doSuccess "[SUCCESS] Succeeded running test #${total}: ${testcase}"
      let "success++"
      echo "\033[92m[SUCCESS] Succeeded running test #${total}: ${testcase}\033[39m" >> ../testResults
    else
      doError "[ERROR] Failed running test #${total}: ${testcase} - exit code ${result}"
      let "failure++"
      echo "\033[91m[ERROR] Failed running test #${total}: ${testcase} - exit code ${result}\033[39m" >> ../testResults
    fi
    let "total++"
    cd ..
  done
}

function doDisplayResults() {
  cat testResults | while read -r line; do echo -e "${line}"; done 
}



function doTests () {
#  mkdir -p ~/.devon
#  touch ~/.devon/.license.agreement
#  rm -rf integration-test
#  mkdir -p integration-test
#  cd integration-test
  #export DEVON_SKIP_PROJECT_SETUP=true
  #doTestsInner "test"
  doTestsInner "integration-test"
  #[[ "${INTEGRATION_TEST}" == true ]] && doTestsInner "integration-test"
  echo -e "\n*****************************************************"
  echo "Executed #${total} test(s), #${success} succeeded and #${failure} failed"
  echo -e "*****************************************************\n"
  if [ "${failure}" == 0 ]
  then
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
    local DEBUG_INTEGRATION_TEST="/c/Users/nmollers/tmp/ideasy-integration-test-debug"
    local HOME="${DEBUG_INTEGRATION_TEST}/home-dir"
    local IDE_ROOT="${HOME}/projects"
    local IDE_BIN="${IDE_ROOT}/_ide/bin"
    local IDE="${DEBUG_INTEGRATION_TEST}/home-dir/projects/_ide/bin/ide"

#    test_files_directory=$(cd `dirname $0` && pwd)
    local test_files_directory=$(realpath $0 | xargs dirname)
    echo "Will run integration tests from dir: ${test_files_directory}"

    rm -rf "${DEBUG_INTEGRATION_TEST}"
    # Only need to mkdir once:
    mkdir -p "${IDE_BIN}"
    

    # TODO remove logs
    echo "IDE_ROOT is: $IDE_ROOT"
    echo "My PWD is: $PWD"
    echo "My ~/ is:" ~/

    # TODO Download latest IDEasy (or build local IDEasy ?)
    # curl  ???
    local SNAPSHOT="/c/Users/nmollers/Downloads/ide-cli-2024.10.001-beta-20241029.023922-8-windows-x64.tar.gz"
    cd "${IDE_ROOT}/_ide"
    tar xfz "${SNAPSHOT}" || (echo "Not able to extract." && exit 1)

    #no need to: cd "${HOME}"
    echo -e "echo \"Tmp home dir's bashrc loading...\"" >> ~/.bashrc
    echo -e "alias ide=\"source ${PWD}/bin/ide\"" >> ~/.bashrc
    # echo -e 'export IDE_ROOT="$(pwd)"' >> ~/.bashrc

    
    # source ./bin/ide
    cd "${IDE_ROOT}"
    doTests
    
    echo "DONE"
    exit 0
}

main

