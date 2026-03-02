echo "Running install intellij integration test"
ide -d install intellij

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/intellij/.ide.software.version" exists

intellij_binary="bin/idea.sh"

if doIsWindows
then
  intellij_binary="bin/idea64.exe"
fi

if doIsMacOs
then
  intellij_binary="idea"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/intellij/${intellij_binary}" exists
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/workspaces/main/idea.properties" exists
