if doIsWindows
then
  touch "$HOME"/.gitconfig
  $IDE -f install
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
