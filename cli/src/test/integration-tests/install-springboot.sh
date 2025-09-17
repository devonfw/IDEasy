echo "Running install springboot (spring-boot-cli) integration test"
ide -d install springboot

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/springboot/bin/spring" exists
