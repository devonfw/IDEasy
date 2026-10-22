echo "Running status integration test"
output=$(ide status)

# ide status may perform update checks (IDEasy and the settings repository) and thus use the network
# when online; only the assertions below are offline-safe (they check stable lines present either way).
assertThat "$output" contains "IDE_ROOT"
assertThat "$output" contains "IDE_HOME"
assertThat "$output" contains "Successfully completed"
