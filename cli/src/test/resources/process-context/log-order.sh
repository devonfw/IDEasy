#!/bin/bash
echo "out1"
read -r -t 1
echo "err1" >&2
read -r -t 1
echo "out2"
read -r -t 1
echo "err2" >&2

