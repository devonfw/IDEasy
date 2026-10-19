echo "Running env integration test"
output=$(ide env)

# ide env prints the resolved environment variables as NAME=VALUE lines.
# PATH is always present; WORKSPACE_PATH is emitted once a workspace is active (as in the test setup).
assertThat "$output" contains "PATH"
assertThat "$output" contains "WORKSPACE_PATH"
