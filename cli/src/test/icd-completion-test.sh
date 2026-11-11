#!/usr/bin/env bash
# Standalone unit test for icd tab-completion (see cli/src/main/package/functions).
# Run directly: bash cli/src/test/icd-completion-test.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FUNCTIONS_FILE="${SCRIPT_DIR}/../main/package/functions"

ideasy() { return 1; }

TEST_ROOT="$(mktemp -d)"
trap 'rm -rf "${TEST_ROOT}"' EXIT

mkdir -p "${TEST_ROOT}/projA/settings" \
  "${TEST_ROOT}/projA/workspaces/main/repo1" \
  "${TEST_ROOT}/projA/workspaces/main/repo2" \
  "${TEST_ROOT}/projA/workspaces/feature1/repo3" \
  "${TEST_ROOT}/projB/settings" \
  "${TEST_ROOT}/projB/workspaces/main" \
  "${TEST_ROOT}/_ide"

export IDE_ROOT="${TEST_ROOT}"
unset IDE_HOME

# shellcheck disable=SC1090
source "${FUNCTIONS_FILE}"

failures=0

assertContains() {
  local description="$1" haystack="$2" needle="$3"
  if [[ " ${haystack} " == *" ${needle} "* ]]; then
    echo "PASS: ${description}"
  else
    echo "FAIL: ${description} - expected '${needle}' in '${haystack}'"
    failures=$((failures + 1))
  fi
}

assertNotContains() {
  local description="$1" haystack="$2" needle="$3"
  if [[ " ${haystack} " == *" ${needle} "* ]]; then
    echo "FAIL: ${description} - did not expect '${needle}' in '${haystack}'"
    failures=$((failures + 1))
  else
    echo "PASS: ${description}"
  fi
}

run_icd_completion() {
  COMP_WORDS=(icd "$@")
  COMP_CWORD=$(( ${#COMP_WORDS[@]} - 1 ))
  _icd_completion
  echo "${COMPREPLY[*]}"
}

# icd -p [Tab] -> suggests all projects, never the internal "_ide" folder
output=$(run_icd_completion -p "")
assertContains "icd -p [Tab] suggests projA" "${output}" "projA"
assertContains "icd -p [Tab] suggests projB" "${output}" "projB"
assertNotContains "icd -p [Tab] does not suggest _ide" "${output}" "_ide"

# icd -p <project> -w [Tab] -> suggests the workspaces of that project
output=$(run_icd_completion -p "projA" -w "")
assertContains "icd -p projA -w [Tab] suggests main" "${output}" "main"
assertContains "icd -p projA -w [Tab] suggests feature1" "${output}" "feature1"

# icd -p <project> -w <workspace> -r [Tab] -> suggests the repositories of that workspace only
output=$(run_icd_completion -p "projA" -w "main" -r "")
assertContains "icd -p projA -w main -r [Tab] suggests repo1" "${output}" "repo1"
assertContains "icd -p projA -w main -r [Tab] suggests repo2" "${output}" "repo2"
assertNotContains "icd -p projA -w main -r [Tab] excludes repo3 from other workspace" "${output}" "repo3"

# icd -p <project> -r [Tab] without -w -> suggests repositories across all workspaces
output=$(run_icd_completion -p "projA" -r "")
assertContains "icd -p projA -r [Tab] (no workspace) suggests repo1" "${output}" "repo1"
assertContains "icd -p projA -r [Tab] (no workspace) suggests repo3" "${output}" "repo3"

# icd -w [Tab] without -p -> project is inferred from IDE_HOME
export IDE_HOME="${TEST_ROOT}/projA"
output=$(run_icd_completion -w "")
assertContains "icd -w [Tab] without -p infers project from IDE_HOME" "${output}" "main"
unset IDE_HOME

# icd [Tab] with no prior option -> suggests the available flags
output=$(run_icd_completion "")
assertContains "icd [Tab] suggests --project flag" "${output}" "--project"
assertContains "icd [Tab] suggests --workspace flag" "${output}" "--workspace"
assertContains "icd [Tab] suggests --repository flag" "${output}" "--repository"

if [ "${failures}" -eq 0 ]; then
  echo "All icd completion tests passed."
  exit 0
else
  echo "${failures} icd completion test(s) failed."
  exit 1
fi
