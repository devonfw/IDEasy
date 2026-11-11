#!/bin/bash

# Writes a marker to stdout — used to verify background processes actually execute.
# The caller should provide $1 as the marker file path.
echo "background-process-ran" > "$1"