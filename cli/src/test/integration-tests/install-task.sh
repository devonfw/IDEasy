echo "Running install task integration test"
ide -d install task

task_location=""
if doIsWindows
then
  task_location=""
else
  task_location="bin/"
fi

# Since #2381, global npm packages are installed into the per-project software/node_modules prefix (not software/node).
# npm places the launcher in the prefix root on Windows but in <prefix>/bin on POSIX (see Npm#setEnvironment).
assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node_modules/${task_location}task" exists
