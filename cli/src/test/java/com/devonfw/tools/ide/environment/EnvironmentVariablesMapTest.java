package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

class EnvironmentVariablesMapTest extends AbstractIdeContextTest {

  private static final String ENVIRONMENT_PROJECT = "environment";

  @Test
  void testMockedEnvironmentVariablesFromPropertiesFile() {
    // Create test context based on a sample project with environment.properties
    Path projectPath = Path.of("src/test/resources/ide-projects/environment/project/home/environment.properties");

    // arrange
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, projectPath.toString(), false);

    // Get the EnvironmentVariablesMap through the context's environment object
    EnvironmentVariables envVars = context.getVariables();
  }
}
