echo "Running install gemini integration test"
ide -d install gemini

gemini_version="$(gemini -v)"

if doIsWindows
then
  gemini_location=""
else
  gemini_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node_modules/${gemini_location}gemini" exists
assertThat "$gemini_version" contains "[0-9.]*[0-9]"
