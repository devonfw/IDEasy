echo "Running install yarn integration test"
ide -d install yarn

yarn_location=""

if doIsWindows
then
  yarn_location=""
else
  yarn_location="bin/"
fi

# Since #2381, global npm packages are installed into the per-project .npm-global prefix (not software/node).
# npm places the launcher in the prefix root on Windows but in <prefix>/bin on POSIX (see Npm#setEnvironment).
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/.npm-global/${yarn_location}yarn" exists
