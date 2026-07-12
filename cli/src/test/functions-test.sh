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
if [ "${failed}" = 0 ]; then
  doSuccess "All ${total} tests passed."
  exit 0
else
  doError "${failed} of ${total} tests failed."
  exit 1
fi
