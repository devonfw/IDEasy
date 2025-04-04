#--- Preamble ---
# Working dir (PWD) is IDE_ROOT (it should contain an ide installation)
# Inherited variables:
#    - $IDE      := IDEasy ($PWD/_ide/bin/ide)
#    - $IDE_ROOT := PWD
#    - All functions in functions-test
# Use 'return <EXIT_CODE>' (instead of exit) to signal if test passed/failed.

# For debugging integration scripts (like this script), it is a good idea to
# fail whenever an error occurred (set -e). When you finished writing the
# test, however, please remove your 'set -e' line, since in production it will
# exit and not run subsequent integration tests. 
#set -e 

#--- Body ---
echo "Running install intellij integration test"
doIdeCreate
$IDE -d install intellij || return 1
return 0

