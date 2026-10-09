echo "Running install ng (Angular) integration test"
ide -d install ng

ng_location=""

if doIsWindows
then
  ng_location=""
else
  ng_location="bin/"
fi

# Since #2381, global npm packages are installed into the per-project software/node_modules prefix (not software/node).
# npm places the launcher in the prefix root on Windows but in <prefix>/bin on POSIX (see Npm#setEnvironment).
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node_modules/${ng_location}ng" exists
