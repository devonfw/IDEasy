echo "Running install cdk integration test"
ide -d install cdk

cdk_location=""

if doIsWindows
then
  cdk_location=""
else
  cdk_location="bin/"
fi

# Since #2381, global npm packages are installed into the per-project software/node_modules prefix (not software/node).
# npm places the launcher in the prefix root on Windows but in <prefix>/bin on POSIX (see Npm#setEnvironment).
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node_modules/${cdk_location}cdk" exists
