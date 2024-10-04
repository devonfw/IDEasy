#!/bin/bash
echo "content to stdout"
echo "more content to stdout"
echo >&2 "error message to stderr"
echo >&2 "another error message to stderr"
return 1
