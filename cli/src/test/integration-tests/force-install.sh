#!/bin/bash

# Integration test to reproduce the issue where force install (-f) breaks environment 
# for subsequent tests when executed in alphabetical order

echo "Running force install integration test to reproduce environment interference issue"

# Capture environment variables before force install
echo "Environment variables before force install:"
env | sort > /tmp/env_before_force.txt

# Run force install to reproduce the issue
echo "Running force install..."
$IDE -f install

# Capture environment variables after force install  
echo "Environment variables after force install:"
env | sort > /tmp/env_after_force.txt

# Show the differences
echo "Environment variable differences:"
diff /tmp/env_before_force.txt /tmp/env_after_force.txt || true

# Test that subsequent IDE commands still work
echo "Testing if subsequent IDE commands work after force install..."
ide_version_output=$($IDE -v 2>&1)
if [[ $? -eq 0 ]]; then
  echo "SUCCESS: IDE version command works after force install"
else
  echo "FAILURE: IDE version command failed after force install"
  echo "Output: $ide_version_output"
fi

# Clean up temporary files
rm -f /tmp/env_before_force.txt /tmp/env_after_force.txt

echo "Force install test completed"