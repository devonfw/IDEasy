echo "Running install spring (spring-boot-cli) integration test"
ide -d install spring

assertThat "${IDE_ROOT}/${TEST_PROJECT_NAME}/software/spring/bin/spring" exists
