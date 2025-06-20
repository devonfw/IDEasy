if doIsWindows
then
  gitconfig_path = "$HOME/.gitconfig"
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
