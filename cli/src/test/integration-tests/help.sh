echo "Running help integration test"
source ../all-tests-functions.sh
output=$(ide help)

assertThat "$output" contains "Current version"
assertThat "$output" contains "build"
assertThat "$output" contains "Usage"
assertThat "$output" contains "docker"
assertThat "$output" contains "--locale"
