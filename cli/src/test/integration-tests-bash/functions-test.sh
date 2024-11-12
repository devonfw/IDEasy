#!/bin/bash

test_project_name="tmp-integ-test"

function doIdeCreate () {
    #TODO: determine the name of the currently executed script
#    local project-name="$(dirname "${BASH_SOURCE:-$0}")" 
#    local project_name="$(basename "${BASH_SOURCE:-$0}")"
#    local test_project_name="tmp-integ-test"
    local settings_url=${1:--}
    echo ide create ${test_project_name} ${settings_url}

    #TODO: IDE_ROOT ?
    mkdir ${IDE_ROOT}/${test_project_name}
    cd ${IDE_ROOT}/${test_project_name}

    # TODO: Remove logs
    echo "END OF doIdeCreate"
    echo "My IDE_ROOT is: ${IDE_ROOT}"
    echo "My PWD is: $PWD"
    echo "settings-url : ${settings_url}"
    echo "project-name : ${project_name}"
}

function doIdeCreateCleanup () {
    rm -rf ${IDE_ROOT}/${test_project_name}
}


function doCommandTest()  {
#  CLI="${PWD}/scripts/devon"
#  "${CLI}" "${1}" --batch setup
  #TODO
  log_setup=">>LOG_COMMAND_TEST.log"
  ide "${@}" "${log_setup}"
  result="${?}"
  exit $result
}

function doExtract() {
  if [ -f "${1}" ]
  then
    tar xfz "${1}" || exit 1
  else
    echo "Could not find and extract release ${1}"
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
