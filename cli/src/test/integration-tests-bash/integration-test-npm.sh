#!/bin/bash

source "$(dirname "${0}")"/functions-test.sh

doIdeCreate 
doCommandTest install npm
retCode=$?
if [ $retCode == 0 ]
then
    doIdeCreateCleanup
fi
exit $retCode
