#--- Preamble ---
# Working dir (PWD) is IDE_ROOT (it should contain an ide installation)
# Inherited variables:
#    - $IDE_ROOT := PWD
#    - All functions in src/test
# Use 'return <EXIT_CODE>' (instead of exit) to signal if test passed/failed.

# For debugging integration scripts (like this script), it is a good idea to
# fail whenever an error occurred (set -e). When you finished writing the
# test, however, please remove your 'set -e' line, since in production it will
# exit and not run subsequent integration tests. 
#set -e 

#--- Body ---
set -e
echo "Running install intellij integration test"
ide -d install intellij
