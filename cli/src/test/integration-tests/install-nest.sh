echo "Running install nest integration test"
ide -d install nest

nest_location=""

if doIsWindows
then
  nest_location=""
else
  nest_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node/${nest_location}nest" exists
