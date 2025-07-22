if doIsWindows
then
  echo "Running ensure git config longpaths gets set integration test"
  touch "$HOME"/.gitconfig
  # Use ide without force flag to reduce environment interference with subsequent tests
  ide install
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
