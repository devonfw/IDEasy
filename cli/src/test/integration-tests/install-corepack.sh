echo "Running install corepack integration test"
ide -d install corepack

corepack_location=""

if doIsWindows
then
  corepack_location=""
else
  corepack_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node/${corepack_location}corepack" exists
