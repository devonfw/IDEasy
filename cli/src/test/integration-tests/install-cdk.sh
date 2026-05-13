echo "Running install cdk integration test"
ide -d install cdk

cdk_location=""

if doIsWindows
then
  cdk_location=""
else
  cdk_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node/${cdk_location}cdk" exists
