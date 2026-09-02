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
# minimal fake "ideasy" binary so sourcing the functions file has no side effects
cat > "${STUB_DIR}/ideasy" <<'EOS'
#!/usr/bin/env bash
exit 0
EOS
chmod +x "${STUB_DIR}/ideasy"
mkdir -p "${STUB_DIR}/root"

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
echo "Testing icd -r repository navigation in ${FUNCTIONS_FILE}"

ICD_ROOT="$(mktemp -d)"
trap 'rm -rf "${STUB_DIR}" "${ICD_ROOT}"' EXIT

# "proj" has a repo directly in main ("cli") and a nested repo in "dev" ("backend/core", with "backend" itself
# also being a valid top-level repo folder in "dev").
mkdir -p "${ICD_ROOT}/proj/workspaces/main/cli"
mkdir -p "${ICD_ROOT}/proj/workspaces/dev/backend/core"
# "proj-named" has a folder matching the project name, for the implicit "-r" fallback.
mkdir -p "${ICD_ROOT}/proj-named/workspaces/main/proj-named"
# "proj-empty" has no repo folders in main at all.
mkdir -p "${ICD_ROOT}/proj-empty/workspaces/main"
# "proj-single" has exactly one non-hidden folder in main (plus a hidden one that must be ignored).
mkdir -p "${ICD_ROOT}/proj-single/workspaces/main/onlyrepo"
mkdir -p "${ICD_ROOT}/proj-single/workspaces/main/.git"
# "proj-multi" has more than one folder in main, so the fallback is ambiguous.
mkdir -p "${ICD_ROOT}/proj-multi/workspaces/main/repoA"
mkdir -p "${ICD_ROOT}/proj-multi/workspaces/main/repoB"

# runIcd <shell> <icd-args...> : sources the functions file in a fresh <shell> with IDE_ROOT=ICD_ROOT
# and prints "<exit code>\n<final PWD>\n<combined stdout/stderr of icd>"
runIcd() {
  local shell_bin="$1"
  shift
  FUNCTIONS_FILE="${FUNCTIONS_FILE}" STUB_DIR="${STUB_DIR}" IDE_ROOT="${ICD_ROOT}" \
    "${shell_bin}" -c '
      export PATH="${STUB_DIR}:${PATH}"
      export IDE_HOME=""
      source "${FUNCTIONS_FILE}" >/dev/null 2>&1
      cd "${IDE_ROOT}" || exit 99
      icd_out="$(mktemp)"
      icd "$@" >"${icd_out}" 2>&1
      code=$?
      printf "%s\n%s\n" "${code}" "${PWD}"
      cat "${icd_out}"
      rm -f "${icd_out}"
    ' _ "$@"
}

# checkIcd <description> <shell> <expected exit code> <expected PWD suffix|""> <expected message substring|""> <icd-args...>
checkIcd() {
  local description="$1" shell_bin="$2" expected_code="$3" expected_pwd_suffix="$4" expected_msg="$5"
  shift 5
  total=$((total + 1))
  local result actual_code actual_pwd actual_msg ok=true
  result="$(runIcd "${shell_bin}" "$@")"
  actual_code="$(printf '%s\n' "${result}" | sed -n '1p')"
  actual_pwd="$(printf '%s\n' "${result}" | sed -n '2p')"
  actual_msg="$(printf '%s\n' "${result}" | tail -n +3)"
  [ "${actual_code}" = "${expected_code}" ] || ok=false
  if [ -n "${expected_pwd_suffix}" ]; then
    case "${actual_pwd}" in
      *"${expected_pwd_suffix}") ;;
      *) ok=false ;;
    esac
  fi
  if [ -n "${expected_msg}" ]; then
    case "${actual_msg}" in
      *"${expected_msg}"*) ;;
      *) ok=false ;;
    esac
  fi
  if [ "${ok}" = true ]; then
    doSuccess "PASSED (${shell_bin}): ${description}"
  else
    doError "FAILED (${shell_bin}): ${description} - exit=${actual_code} (expected ${expected_code}), pwd=${actual_pwd} (expected suffix '${expected_pwd_suffix}'), msg=${actual_msg}"
    failed=$((failed + 1))
  fi
}

ICD_SHELLS=(bash)
if command -v zsh >/dev/null 2>&1; then
  ICD_SHELLS+=(zsh)
else
  echo "zsh not found on PATH - skipping zsh regression checks for icd -r (bash still covered)"
fi

for icd_shell in "${ICD_SHELLS[@]}"; do
  checkIcd "-r <repo>: found directly in main" "${icd_shell}" 0 "/proj/workspaces/main/cli" "" \
    -p proj -r cli
  checkIcd "-r <repo>: not in main, found in another workspace" "${icd_shell}" 0 "/proj/workspaces/dev/backend" "" \
    -p proj -r backend
  checkIcd "-r <repo>: nested repository path within an explicit workspace" "${icd_shell}" 0 "/proj/workspaces/dev/backend/core" "" \
    -p proj -w dev -r backend/core
  checkIcd "-w restricts the search to that single workspace" "${icd_shell}" 1 "" "not found in workspace 'dev'" \
    -p proj -w dev -r cli
  checkIcd "-r <repo>: not found anywhere -> error" "${icd_shell}" 1 "" "not found in any workspace" \
    -p proj -r nope
  checkIcd "-r <repo>: explicit repo, nonexistent project -> error, no crash" "${icd_shell}" 1 "" "not found in any workspace" \
    -p no-such-project -r foo
  checkIcd "-r (implicit): resolves the project-name folder" "${icd_shell}" 0 "/proj-named/workspaces/main/proj-named" "" \
    -p proj-named -r
  checkIcd "-r (implicit): single unambiguous folder in main is used, hidden folder ignored" "${icd_shell}" 0 "/proj-single/workspaces/main/onlyrepo" "" \
    -p proj-single -r
  checkIcd "-r (implicit): multiple folders in main -> warning, cd into workspace" "${icd_shell}" 0 "/proj-multi/workspaces/main" "not contain a single unambiguous repository" \
    -p proj-multi -r
  checkIcd "-r (implicit): empty workspace -> warning, cd into workspace, no crash" "${icd_shell}" 0 "/proj-empty/workspaces/main" "not contain a single unambiguous repository" \
    -p proj-empty -r
done

echo
if [ "${failed}" = 0 ]; then
  doSuccess "All ${total} tests passed."
  exit 0
else
  doError "${failed} of ${total} tests failed."
  exit 1
fi
