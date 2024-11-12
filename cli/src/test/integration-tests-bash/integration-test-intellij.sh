#!/bin/bash

source "$(dirname "${0}")"/functions-test.sh
doIdeCreate 
doCommandTest gibberish whatever intellij
retCode=$?
if [ $retCode == 0 ]
then
    # TODO: else rename folder?
    doIdeCreateCleanup
fi
exit $retCode
