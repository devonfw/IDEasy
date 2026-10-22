echo "Running version integration test"
output=$(ide version)

# ide version prints the IDEasy version string itself (e.g. "2026.08.001").
# It must therefore be non-empty and match the value reported by the -v option.
if [ -z "$output" ]; then
  doError "ide version returned an empty string"
  integration_test_result=1
fi
assertThat "$output" equals "$(ide -v)"
