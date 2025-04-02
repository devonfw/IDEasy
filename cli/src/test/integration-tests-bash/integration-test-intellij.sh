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
echo "My ide $IDE"
echo "MY PWD: $PWD"
#doIdeCreate 
echo "MY PWD: $PWD"

doWarning "Fn working?"
#cd asgkagkadgöadjg
#cd agagagadlöm
#which ide | echo 
#$IDE gibberish whatever intellij 
#cd sometakntnalgnank || return 1
# cd sometakntnalgnank
#echo "Try and fail" && exit 1
echo "All good."
#exit 0

