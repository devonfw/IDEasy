echo "Running install nestjs integration test"
ide -d install nestjs

nestjs_location=""

if doIsWindows
then
  nestjs_location=""
else
  nestjs_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node/${nestjs_location}nestjs" exists
