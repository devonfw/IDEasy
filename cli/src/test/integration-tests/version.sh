echo "Running version integration test"
output=$(ide version)

# ide version prints the IDEasy version string itself (e.g. "2026.08.001" or "2026.10.001-10_28_07-SNAPSHOT").
assertThat "$output" contains '^[0-9]\{4\}\.[0-9]\{2\}\.[0-9]\{3\}'
assertThat "$output" equals "$(ide -v)"
