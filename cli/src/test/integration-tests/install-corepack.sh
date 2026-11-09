echo "Running install corepack integration test"
ide -d install corepack

corepack_location=""

if doIsWindows
then
  corepack_location=""
else
  corepack_location="bin/"
fi

# Since #2381, global npm packages are installed into the per-project software/node_modules prefix (not software/node).
# npm places the launcher in the prefix root on Windows but in <prefix>/bin on POSIX (see Npm#setEnvironment).
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node_modules/${corepack_location}corepack" exists
