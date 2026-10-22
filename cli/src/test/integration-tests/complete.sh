echo "Running complete integration test"
output=$(ide complete "")

# ide complete "" returns the list of top-level completion candidates (flags and commands)
assertThat "$output" contains "install"
assertThat "$output" contains "version"
