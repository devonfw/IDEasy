echo "Running install task integration test"
ide -d install task

task_location=""
if doIsWindows
then
  task_location=""
else
  task_location="bin/"
fi

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/node/${task_location}task" exists
