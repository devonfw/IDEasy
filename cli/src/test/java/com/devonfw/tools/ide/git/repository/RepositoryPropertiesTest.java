package com.devonfw.tools.ide.git.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link RepositoryProperties}.
 */
class RepositoryPropertiesTest {

  @Test
  void testGetId() {

    assertThat(properties("settings.properties").getId()).isEqualTo("settings");
    assertThat(properties("foo.properties").getId()).isEqualTo("foo");
    assertThat(properties("foo").getId()).isEqualTo("foo");
  }

  @Test
  void testIsSettingsProperties() {

    RepositoryProperties settingsProperties = properties("settings.properties");
    assertThat(settingsProperties.getGitUrl()).isNull();
    assertThat(settingsProperties.isInvalid()).isFalse();

    RepositoryProperties fooProperties = properties("foo.properties");
    assertThat(fooProperties.getGitUrl()).isNull();
    assertThat(fooProperties.isInvalid()).isTrue();
  }

  private static RepositoryProperties properties(String filename) {

    return new RepositoryProperties(Path.of(filename), new Properties());
  }
}
