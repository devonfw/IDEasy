echo "Running simulate user input during installation integration test"

echo "Simulate user input yes to ide install prompt"
output=$(echo "1" | $IDE install 2>&1) || true
if echo "$output" | grep -q "Aborted by end-user"; then
  echo "Test 1 FAILED: User input '1' should not abort"
  exit 1
fi
assertThat "$output" contains "installed successfully"
output=$(echo "yes" | $IDE install 2>&1) || true
if echo "$output" | grep -q "Aborted by end-user"; then
  echo "Test 3 FAILED: User input 'yes' should not abort"
  exit 1
fi
assertThat "$output" contains "installed successfully"

echo "Simulate user input no to ide install prompt"
output=$(echo "2" | $IDE install 2>&1) || true
assertThat "$output" contains "Aborted by end-user"
output=$(echo "no" | $IDE install 2>&1) || true
assertThat "$output" contains "Aborted by end-user"

echo "Simulate invalid user input to ide install prompt"
output=$(echo "invalid" | $IDE install 2>&1) || true
assertThat "$output" contains "Invalid answer"
assertThat "$output" contains "invalid"
