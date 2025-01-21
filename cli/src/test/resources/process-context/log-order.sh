#!/bin/bash
echo "out1"
read -r -t 1 -p ""
echo "err1" >&2
read -r -t 1 -p ""
echo "out2"
read -r -t 1 -p ""
echo "err2" >&2
