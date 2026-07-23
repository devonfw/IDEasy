echo "Running icd completion integration test"

# additional workspace and repositories used only to verify completion candidates
mkdir -p "${IDE_ROOT}/${TEST_PROJECT_NAME}/workspaces/main/repo-alpha"
mkdir -p "${IDE_ROOT}/${TEST_PROJECT_NAME}/workspaces/feature-x/repo-beta"

# a second project so project completion has more than one candidate
mkdir -p "${IDE_ROOT}/other-project/settings" "${IDE_ROOT}/other-project/workspaces/main"

# simulates pressing [Tab] for "icd $*" and echoes the resulting COMPREPLY
run_icd_completion() {
  COMP_WORDS=(icd "$@")
  COMP_CWORD=$(( ${#COMP_WORDS[@]} - 1 ))
  _icd_completion
  echo "${COMPREPLY[*]}"
}

# icd -p [Tab] -> suggests all projects
output=$(run_icd_completion -p "")
assertThat "$output" contains "${TEST_PROJECT_NAME}"
assertThat "$output" contains "other-project"

# icd -p <project> -w [Tab] -> suggests the workspaces of that project
output=$(run_icd_completion -p "${TEST_PROJECT_NAME}" -w "")
assertThat "$output" contains "main"
assertThat "$output" contains "feature-x"

# icd -p <project> -w <workspace> -r [Tab] -> suggests the repositories of that workspace
output=$(run_icd_completion -p "${TEST_PROJECT_NAME}" -w "main" -r "")
assertThat "$output" contains "repo-alpha"

# icd -p <project> -r [Tab] (no workspace given) -> suggests repositories across all workspaces
output=$(run_icd_completion -p "${TEST_PROJECT_NAME}" -r "")
assertThat "$output" contains "repo-alpha"
assertThat "$output" contains "repo-beta"

# icd -w [Tab] without -p -> project is inferred from the current directory (set up by doIdeCreate)
output=$(run_icd_completion -w "")
assertThat "$output" contains "main"

# icd [Tab] with no prior option -> suggests the available flags
output=$(run_icd_completion "")
assertThat "$output" contains "--project"
assertThat "$output" contains "--workspace"
assertThat "$output" contains "--repository"

# cleanup fixtures created for this test
rm -rf "${IDE_ROOT}/${TEST_PROJECT_NAME}/workspaces/feature-x" "${IDE_ROOT}/${TEST_PROJECT_NAME}/workspaces/main/repo-alpha" "${IDE_ROOT}/other-project"
