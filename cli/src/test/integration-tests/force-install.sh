#!/bin/bash

# Integration test to reproduce the issue where force install (-f) breaks environment 
# for subsequent tests when executed in alphabetical order

echo "Running force install integration test to reproduce environment interference issue"

# Capture initial environment state
echo "=== Initial Environment State ==="
env | grep -E "IDE|PATH" | sort > /tmp/env_initial.txt
echo "IDE_ROOT: '$IDE_ROOT'"
echo "PATH: '$PATH'"

# Create a dummy .gitconfig file for testing
touch "$HOME"/.gitconfig

# Capture environment variables before force install
echo "=== Environment before force install ==="
env | sort > /tmp/env_before_force.txt
echo "Shell RC files before:"
echo "~/.bashrc exists: $(test -f ~/.bashrc && echo "yes" || echo "no")"
if [ -f ~/.bashrc ]; then
  echo "Last 20 lines of ~/.bashrc:"
  tail -20 ~/.bashrc
fi

# Run force install to reproduce the issue
echo "=== Running force install ==="
$IDE -f install

# Capture environment variables after force install  
echo "=== Environment after force install ==="
env | sort > /tmp/env_after_force.txt
echo "Shell RC files after:"
echo "~/.bashrc exists: $(test -f ~/.bashrc && echo "yes" || echo "no")" 
if [ -f ~/.bashrc ]; then
  echo "Last 20 lines of ~/.bashrc:"
  tail -20 ~/.bashrc
fi

# Show the differences
echo "=== Environment variable differences ==="
diff /tmp/env_before_force.txt /tmp/env_after_force.txt || true

# Test that the git longpaths configuration gets set (original test purpose)
echo "=== Testing git longpaths configuration ==="
if doIsWindows; then
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
else
  echo "Skipping git longpaths test - only applicable on Windows"
fi

# Test that subsequent IDE commands still work
echo "=== Testing subsequent IDE commands ==="
ide_version_output=$($IDE -v 2>&1)
if [[ $? -eq 0 ]]; then
  echo "SUCCESS: IDE version command works after force install"
  echo "Version output: $ide_version_output"
else
  echo "FAILURE: IDE version command failed after force install"
  echo "Error output: $ide_version_output"
fi

# Test ide env command
echo "=== Testing ide env command ==="
ide_env_output=$($IDE env --bash 2>&1)
if [[ $? -eq 0 ]]; then
  echo "SUCCESS: IDE env command works after force install"
  echo "Environment variables count: $(echo "$ide_env_output" | wc -l)"
else
  echo "FAILURE: IDE env command failed after force install" 
  echo "Error output: $ide_env_output"
fi

# Clean up temporary files
rm -f /tmp/env_initial.txt /tmp/env_before_force.txt /tmp/env_after_force.txt

echo "Force install test completed"
