echo "Running install intellij integration test"
ide -d install intellij

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/intellij/.ide.software.version" exists

intellij_binary="idea.sh"

if doIsWindows
then
  intellij_binary="idea64.exe"
fi

if doIsMacOs
then
  intellij_binary="idea"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/intellij/bin/${intellij_binary}" exists
