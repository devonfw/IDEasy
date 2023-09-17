package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.log.IdeSlf4jRootLogger;

/**
 * Test of {@link EnvironmentVariablesPropertiesFile}.
 */
@SuppressWarnings("javadoc")
class EnvironmentVariablesPropertiesFileTest extends Assertions {

  private static final IdeSlf4jRootLogger LOGGER = IdeSlf4jRootLogger.of();

  /**
   * Test of {@link EnvironmentVariablesPropertiesFile} including legacy support.
   */
  @Test
  public void testLoad() {

    // arrange
    AbstractEnvironmentVariables parent = null;
    Path propertiesFilePath = Path.of("src/test/resources/com/devonfw/tools/ide/env/var/devon.properties");
    EnvironmentVariablesType type = EnvironmentVariablesType.SETTINGS;
    // act
    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(parent, type,
        propertiesFilePath, LOGGER);
    // assert
    assertThat(variables.getType()).isSameAs(type);
    assertThat(variables.get("MVN_VERSION")).isEqualTo("3.9.0");
    assertThat(variables.get("IDE_TOOLS")).isEqualTo("mvn, npm");
    assertThat(variables.get("CREATE_START_SCRIPTS")).isEqualTo("eclipse");
    assertThat(variables.get("KEY")).isEqualTo("value");
    assertThat(variables.getVariables()).hasSize(4);
  }

}
