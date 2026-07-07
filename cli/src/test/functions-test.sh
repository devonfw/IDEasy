#!/usr/bin/env bash
# Unit tests for the shell integration in cli/src/main/package/functions.
#
# These tests focus on the OS-detection behavior that decides whether the
# "CYGWIN IS NOT SUPPORTED" warning is shown. Since 2025-02 MSYS2 (and thus
# Git Bash) reports OSTYPE=cygwin, so OSTYPE alone can no longer distinguish
# Cygwin from Git Bash - the detection has to rely on "uname -s" instead.
#
# Run standalone: bash cli/src/test/functions-test.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "${0}")" && pwd)"
FUNCTIONS_FILE="${SCRIPT_DIR}/../main/package/functions"

# reuse the assertThat helper of the integration tests
# shellcheck source=all-tests-functions.sh
source "${SCRIPT_DIR}/all-tests-functions.sh"

STUB_DIR="$(mktemp -d)"
trap 'rm -rf "${STUB_DIR}"' EXIT
# fake "ideasy" binary. For the "env" subcommand it derives IDE_HOME, WORKSPACE and WORKSPACE_PATH
# from the current directory just like the real binary would, so that the icd message tests below
# can observe the project-root vs workspace behavior. For anything else it is a no-op.
cat > "${STUB_DIR}/ideasy" <<'EOS'
#!/usr/bin/env bash
case "$*" in
  *env*)
    home=""
    dir="${PWD}"
    while [ -n "${dir}" ] && [ "${dir}" != "/" ]; do
      if [ -d "${dir}/workspaces" ] && [ -d "${dir}/settings" ]; then
        home="${dir}"
        break
      fi
      dir="$(dirname "${dir}")"
    done
    if [ -n "${home}" ]; then
      workspace="main"
      relative="${PWD#"${home}/"}"
      if [ "${relative}" != "${PWD}" ]; then
        case "${relative}" in
          workspaces/*)
            workspace="${relative#workspaces/}"
            workspace="${workspace%%/*}"
            ;;
        esac
      fi
      echo "IDE_HOME=${home}"
      echo "WORKSPACE=${workspace}"
      echo "WORKSPACE_PATH=${home}/workspaces/${workspace}"
    fi
    ;;
esac
exit 0
EOS
chmod +x "${STUB_DIR}/ideasy"
# fake IDEasy project so the stub above recognizes it as an IDE_HOME
mkdir -p "${STUB_DIR}/root/project/settings"
mkdir -p "${STUB_DIR}/root/project/workspaces/main"
mkdir -p "${STUB_DIR}/root/project/workspaces/foo"

total=0
failed=0

# runIde <OSTYPE> <uname -s value> : sources the functions file in a fresh shell that
# simulates the given environment and prints the output of "ide <arg>".
runIde() {
  FAKE_OSTYPE="$1" FAKE_UNAME_S="$2" FUNCTIONS_FILE="${FUNCTIONS_FILE}" STUB_DIR="${STUB_DIR}" \
    bash --noprofile --norc -c '
      export PATH="${STUB_DIR}:${PATH}"
      export IDE_ROOT="${STUB_DIR}/root"
      uname() { if [ "$1" = "-s" ]; then echo "${FAKE_UNAME_S}"; else command uname "$@"; fi; }
      cygpath() { echo "${@: -1}"; }
      OSTYPE="${FAKE_OSTYPE}"
      source "${FUNCTIONS_FILE}" >/dev/null 2>&1
      ide dummyarg 2>&1
    '
}

# check <description> <expected: shown|hidden> <OSTYPE> <uname -s value>
check() {
  local description="$1" expected="$2" ostype="$3" uname_s="$4"
  total=$((total + 1))
  local output
  output="$(runIde "${ostype}" "${uname_s}")"
  if echo "${output}" | grep -q "CYGWIN IS NOT SUPPORTED"; then
    local actual="shown"
  else
    local actual="hidden"
  fi
  if [ "${actual}" = "${expected}" ]; then
    doSuccess "PASSED: ${description} (warning ${actual})"
  else
    doError "FAILED: ${description} - expected warning ${expected} but was ${actual}"
    failed=$((failed + 1))
  fi
}

# runIcd <initial IDE_HOME> <icd args...> : sources the functions file in a fresh shell, exports
# IDE_ROOT (and IDE_HOME if given) and prints the output of "icd <args>".
runIcd() {
  local initial_home="$1"
  shift
  # strip any inherited IDEasy state (and BASH_ENV, which may re-initialize it) so the test is hermetic
  FUNCTIONS_FILE="${FUNCTIONS_FILE}" STUB_DIR="${STUB_DIR}" INITIAL_HOME="${initial_home}" \
    env -u IDE_HOME -u WORKSPACE -u WORKSPACE_PATH -u IDE_OPTIONS -u BASH_ENV \
    bash --noprofile --norc -c '
      export PATH="${STUB_DIR}:${PATH}"
      export IDE_ROOT="${STUB_DIR}/root"
      cygpath() { echo "${@: -1}"; }
      # source from the (project-less) IDE_ROOT so the "ide" call at the end of the functions file
      # does not pick up a surrounding IDEasy project, then set the desired state for the test.
      cd "${IDE_ROOT}" || exit 1
      source "${FUNCTIONS_FILE}" >/dev/null 2>&1
      unset IDE_HOME WORKSPACE WORKSPACE_PATH
      [ -n "${INITIAL_HOME}" ] && export IDE_HOME="${INITIAL_HOME}"
      icd "$@" 2>&1
    ' _ "$@"
}

# checkIcdInWorkspace <description> <expected substring> <initial IDE_HOME> <icd args...>
checkIcdInWorkspace() {
  local description="$1" expected="$2" initial_home="$3"
  shift 3
  total=$((total + 1))
  local output
  output="$(runIcd "${initial_home}" "$@")"
  if echo "${output}" | grep -qF "${expected}"; then
    doSuccess "PASSED: ${description}"
  else
    doError "FAILED: ${description} - expected to contain '${expected}' but was '${output}'"
    failed=$((failed + 1))
  fi
}

# checkIcdProjectRoot <description> <initial IDE_HOME> <icd args...> : asserts the message reports the
# project home without mentioning any workspace (see #1808).
checkIcdProjectRoot() {
  local description="$1" initial_home="$2"
  shift 2
  total=$((total + 1))
  local output
  output="$(runIcd "${initial_home}" "$@")"
  if echo "${output}" | grep -qF "have been set for" && ! echo "${output}" | grep -q "workspace"; then
    doSuccess "PASSED: ${description}"
  else
    doError "FAILED: ${description} - expected a project-root message without any workspace but was '${output}'"
    failed=$((failed + 1))
  fi
}

# checkIcdNoSetup <description> <initial IDE_HOME> <icd args...> : asserts that no environment is set
# up and no "have been set" message is printed (icd only navigates to the projects root, see #1808).
checkIcdNoSetup() {
  local description="$1" initial_home="$2"
  shift 2
  total=$((total + 1))
  local output
  output="$(runIcd "${initial_home}" "$@")"
  if ! echo "${output}" | grep -q "have been set"; then
    doSuccess "PASSED: ${description}"
  else
    doError "FAILED: ${description} - expected no environment setup message but was '${output}'"
    failed=$((failed + 1))
  fi
}

echo "Testing project-root vs workspace message via icd in ${FUNCTIONS_FILE}"

# icd -p navigates to the project root, so the message must not mention any workspace at all (see #1808)
checkIcdProjectRoot "icd -p project reports the project home without any workspace" \
  "" -p project
# icd -p -w navigates into the (default main) workspace, so the established "in workspace <name>" wording is kept
checkIcdInWorkspace "icd -p project -w reports 'in workspace main'" \
  "in workspace main" "" -p project -w
# icd -p -w <name> navigates into the named workspace
checkIcdInWorkspace "icd -p project -w foo reports 'in workspace foo'" \
  "in workspace foo" "" -p project -w foo
# inside a project, bare icd jumps to IDE_HOME (the project root), so it must not mention any workspace
checkIcdProjectRoot "icd (no args, inside a project) reports the project home without any workspace" \
  "${STUB_DIR}/root/project"
# outside any project, bare icd only navigates to IDE_ROOT (the projects root) without any env setup or message
checkIcdNoSetup "icd (no args, no project) navigates to the projects root without any setup message" \
  ""

echo
echo "Testing Cygwin warning detection in ${FUNCTIONS_FILE}"

# genuine Cygwin -> warning is intended
check "genuine Cygwin console shows the warning" shown cygwin "CYGWIN_NT-10.0-26200"
# modern Git Bash: MSYS2 relabeled OSTYPE to cygwin -> must NOT warn (the regression)
check "modern Git Bash (OSTYPE=cygwin, uname=MINGW) shows no warning" hidden cygwin "MINGW64_NT-10.0-26200"
# legacy Git Bash: OSTYPE still msys -> no warning
check "legacy Git Bash (OSTYPE=msys, uname=MINGW) shows no warning" hidden msys "MINGW64_NT-10.0-26200"
# MSYS environment -> no warning
check "MSYS environment shows no warning" hidden cygwin "MSYS_NT-10.0-26200"

echo
if [ "${failed}" = 0 ]; then
  doSuccess "All ${total} tests passed."
  exit 0
else
  doError "${failed} of ${total} tests failed."
  exit 1
fi
