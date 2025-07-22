if doIsWindows
then
  echo "Running ensure git config longpaths gets set integration test"
  touch "$HOME"/.gitconfig
  # Use debug mode install instead of force mode to reduce environment interference
  # This still triggers the IDEasy installation but is less intrusive than force mode
  ide -d install
  gitconfig_path="$HOME"/.gitconfig
  fileContent=$(cat "$gitconfig_path")
  assertThat "$fileContent" contains "longpaths"
fi
