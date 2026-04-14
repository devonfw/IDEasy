#!/bin/bash

# Integration test to verify that license agreement is created when accepted
# by user input as well as otherwise when declined

LICENSE_FILE="$HOME/.ide/.license.agreement"

# Usage: run_test <input> <expected_behavior>
run_test() {
    local input=$1
    local behavior=$2

    echo "Running case: input '$input' should $behavior"

    rm -f "$LICENSE_FILE"
    output=$(echo "$input" | "$IDE" -f install 2>&1) || true

    # Verify the prompt always appears
    if echo "$output" | grep -q "Do you accept these terms"; then
        doSuccess "Prompt printed"
    else
        doError "Prompt missing"
    fi

    if [ "$behavior" == "accept" ]; then
        # Verify the license file was created
        if [ -f "$LICENSE_FILE" ]; then
            doSuccess "License file was created."
        else
            doError "License file is missing (it should have been created)."
        fi
    else
        # Verify the license file was not created
        if [ ! -f "$LICENSE_FILE" ]; then
            doSuccess "License file was correctly not created."
        else
            doError "License file exists (it should not have been created)."
        fi
        # Verify the installation reported an abort
        if echo "$output" | grep -q "Aborted"; then
            doSuccess "Installation was aborted correctly."
        else
            doError "Installation did not abort as expected."
        fi
    fi
}

echo "Running license agreement required during installation integration test"

run_test "yes" "accept"
run_test "1"   "accept"
run_test "no"  "decline"
run_test "2"   "decline"

