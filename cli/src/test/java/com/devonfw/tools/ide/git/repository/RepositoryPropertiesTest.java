package com.devonfw.tools.ide.git.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
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

  private static RepositoryProperties properties(String filename, Properties properties) {

    return new RepositoryProperties(Path.of(filename), properties);
  }

  @Test
  void testGetRemotesEmpty() {

    RepositoryProperties props = properties("test.properties");
    assertThat(props.getRemotes()).isEmpty();
  }

  @Test
  void testGetRemotesSingle() {

    Properties props = new Properties();
    props.setProperty("git_remote", "upstream:https://github.com/devonfw/ide-settings.git");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo("upstream");
    assertThat(remotes.get(0).url()).isEqualTo("https://github.com/devonfw/ide-settings.git");
  }

  @Test
  void testGetRemotesMultiple() {

    Properties props = new Properties();
    props.setProperty("git_remote", "upstream:https://github.com/devonfw/upstream.git,fork:https://github.com/user/fork.git");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(2);
    assertThat(remotes.get(0).name()).isEqualTo("upstream");
    assertThat(remotes.get(0).url()).isEqualTo("https://github.com/devonfw/upstream.git");
    assertThat(remotes.get(1).name()).isEqualTo("fork");
    assertThat(remotes.get(1).url()).isEqualTo("https://github.com/user/fork.git");
  }

  @Test
  void testGetRemotesInvalidEntriesSkipped() {

    Properties props = new Properties();
    props.setProperty("git_remote", "badentry,noColon,good:https://example.com/repo.git");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo("good");
    assertThat(remotes.get(0).url()).isEqualTo("https://example.com/repo.git");
  }

  @Test
  void testGetRemotesWithWhitespace() {

    Properties props = new Properties();
    props.setProperty("git_remote", "  upstream : https://github.com/devonfw/repo.git ");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo("upstream");
    assertThat(remotes.get(0).url()).isEqualTo("https://github.com/devonfw/repo.git");
  }
}
