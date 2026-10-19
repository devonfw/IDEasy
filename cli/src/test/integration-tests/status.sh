echo "Running status integration test"
output=$(ide status)

assertThat "$output" contains "IDE_ROOT"
assertThat "$output" contains "IDE_HOME"
assertThat "$output" contains "Successfully completed"
