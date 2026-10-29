echo "Running env integration test"
output=$(ide env)

# ide env prints the resolved environment variables as NAME=VALUE lines.
# Anchor PATH to the start of the line, or to a leading space (as in "export PATH=..." that ide env emits on
# Linux/macOS), and include its '=' so the assertion really matches the PATH variable and is not satisfied by
# WORKSPACE_PATH, which also contains the substring "PATH". WORKSPACE_PATH is emitted once a workspace is active
# (as in the test setup).
assertThat "$output" contains '\(^\|[ ]\)PATH='
assertThat "$output" contains "WORKSPACE_PATH"
