#--- Body ---
echo "Running help integration test"
output=$(ide help)

assertThat() {
  local output="$1"
  local keyword="$2"

  if echo "$output" | grep -q "$keyword"; then
    echo "Assertion passed: '$keyword' found in output"
    return 0
  else
    echo "Assertion failed: '$keyword' not found in output"
    return 1
  fi
}

assertThat "$output" "Current version"
assertThat "$output" "build"
assertThat "$output" "Usage"
assertThat "$output" "docker"
assertThat "$output" "--locale"
