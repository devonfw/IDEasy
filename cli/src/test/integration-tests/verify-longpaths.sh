if doIsWindows
then
  echo "Running ensure git config longpaths gets set integration test"
  touch "$HOME"/.gitconfig
  $IDE -f install
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
