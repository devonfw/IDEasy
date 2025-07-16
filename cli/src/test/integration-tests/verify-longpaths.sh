if doIsWindows
then
  echo "Running ensure git config longpaths gets set integration test"
  touch "$HOME"/.gitconfig
  # Test longpaths without doing full IDEasy installation that interferes with other tests
  # Use the normal install command instead of forced install to avoid environment conflicts
  ide -d install
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
