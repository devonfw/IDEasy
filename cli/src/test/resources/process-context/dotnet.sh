#!/bin/bash
echo "content to stdout"
echo >&2 "error message to stderr"
echo "more content to stdout"
echo >&2 "another error message to stderr"
sleep 3
exit 2
