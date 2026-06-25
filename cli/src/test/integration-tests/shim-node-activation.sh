#!/bin/bash

echo "Running shim based node activation integration test"

if ! doIsWindows; then
  doWarning "Skipping shim-node-activation.sh because Windows .cmd shims are tested only on Windows for now."
  return 0
fi

# Optional guard for spike/research mode.
# Remove this guard later if the test should run by default in CI.
if [ "${IDEASY_SHIM_INTEGRATION_TEST}" != "true" ]; then
  doWarning "Skipping shim-node-activation.sh because IDEASY_SHIM_INTEGRATION_TEST=true is not set."
  return 0
fi

doIdeCreate

echo "Ensuring node is installed via IDEasy"
node_output_before="$(ide node -v 2>&1)"
node_rc_before=$?

if [ "${node_rc_before}" -ne 0 ]; then
  doError "Failed to install/run node via IDEasy before shim test:"
  echo "${node_output_before}"
  return 1
fi

echo "Node via IDEasy before shim:"
echo "${node_output_before}"

SHIM_TEST_DIR="${IDE_ROOT}/shim-node-activation-test"
SHIMS_DIR="${SHIM_TEST_DIR}/shims"
IDE_BIN_DIR="${SHIM_TEST_DIR}/ide-bin"

mkdir -p "${SHIMS_DIR}"
mkdir -p "${IDE_BIN_DIR}"

echo "Creating temporary ide.cmd bridge"
cat > "${IDE_BIN_DIR}/ide.cmd" <<EOF
@echo off
"$(cygpath -w "${IDE}")" %*
exit /b %ERRORLEVEL%
EOF

echo "Creating temporary node.cmd shim"
cat > "${SHIMS_DIR}/node.cmd" <<'EOF'
@echo off
ide node %*
exit /b %ERRORLEVEL%
EOF

WIN_SHIMS_DIR="$(cygpath -w "${SHIMS_DIR}")"
WIN_IDE_BIN_DIR="$(cygpath -w "${IDE_BIN_DIR}")"

echo "Running node -v through experimental shim"
shim_output="$(cmd //c "set PATH=${WIN_SHIMS_DIR};${WIN_IDE_BIN_DIR};%PATH%&& where node&& node -v" 2>&1)"
shim_rc=$?

echo "${shim_output}"

if [ "${shim_rc}" -ne 0 ]; then
  doError "node -v through shim failed with exit code ${shim_rc}"
  return 1
fi

echo "${shim_output}" | grep -E "v[0-9]+\.[0-9]+\.[0-9]+" >/dev/null
if [ "$?" -ne 0 ]; then
  doError "node -v through shim did not print a Node version"
  return 1
fi

echo "${shim_output}" | grep -i "shim-node-activation-test.*shims.*node.cmd" >/dev/null
if [ "$?" -ne 0 ]; then
  doWarning "where node output did not clearly show the temporary shim first. Output was:"
  echo "${shim_output}"
fi

doSuccess "node -v through experimental shim succeeded"

return 0
