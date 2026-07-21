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

  // Test remote names
  private static final String REMOTE_NAME_UPSTREAM = "upstream";
  private static final String REMOTE_NAME_FORK = "fork";
  private static final String REMOTE_NAME_GOOD = "good";
  private static final String REMOTE_NAME_VALID = "valid";

  // Test remote URLs
  private static final String URL_DEVONFW_SETTINGS = "https://github.com/devonfw/ide-settings.git";
  private static final String URL_DEVONFW_UPSTREAM = "https://github.com/devonfw/upstream.git";
  private static final String URL_DEVONFW_REPO = "https://github.com/devonfw/repo.git";
  private static final String URL_USER_FORK = "https://github.com/user/fork.git";
  private static final String URL_EXAMPLE_REPO = "https://example.com/repo.git";
  private static final String URL_SSH_DEVONFW = "git@github.com:devonfw/ide-settings.git";

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
    props.setProperty("git_remote", REMOTE_NAME_UPSTREAM + ":" + URL_DEVONFW_SETTINGS);
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_UPSTREAM);
    assertThat(remotes.get(0).url()).isEqualTo(URL_DEVONFW_SETTINGS);
  }

  @Test
  void testGetRemotesMultiple() {

    Properties props = new Properties();
    props.setProperty("git_remote", REMOTE_NAME_UPSTREAM + ":" + URL_DEVONFW_UPSTREAM + "," + REMOTE_NAME_FORK + ":" + URL_USER_FORK);
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(2);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_UPSTREAM);
    assertThat(remotes.get(0).url()).isEqualTo(URL_DEVONFW_UPSTREAM);
    assertThat(remotes.get(1).name()).isEqualTo(REMOTE_NAME_FORK);
    assertThat(remotes.get(1).url()).isEqualTo(URL_USER_FORK);
  }

  @Test
  void testGetRemotesInvalidEntriesSkipped() {

    Properties props = new Properties();
    props.setProperty("git_remote", "badentry,noColon," + REMOTE_NAME_GOOD + ":" + URL_EXAMPLE_REPO);
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_GOOD);
    assertThat(remotes.get(0).url()).isEqualTo(URL_EXAMPLE_REPO);
  }

  @Test
  void testGetRemotesWithWhitespace() {

    Properties props = new Properties();
    props.setProperty("git_remote", "  " + REMOTE_NAME_UPSTREAM + " : " + URL_DEVONFW_REPO + " ");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_UPSTREAM);
    assertThat(remotes.get(0).url()).isEqualTo(URL_DEVONFW_REPO);
  }

  @Test
  void testGetRemotesInvalidNameRejected() {

    Properties props = new Properties();
    props.setProperty("git_remote",
        "my-remote:https://a.git,remote2:https://b.git,remote_one:https://c.git," + REMOTE_NAME_VALID + ":https://d.git");
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_VALID);
    assertThat(remotes.get(0).url()).isEqualTo("https://d.git");
  }

  @Test
  void testGetRemotesSshUrl() {

    Properties props = new Properties();
    props.setProperty("git_remote", REMOTE_NAME_UPSTREAM + ":" + URL_SSH_DEVONFW);
    RepositoryProperties repositoryProperties = properties("test.properties", props);

    List<com.devonfw.tools.ide.git.repository.RepositoryRemote> remotes = repositoryProperties.getRemotes();
    assertThat(remotes).hasSize(1);
    assertThat(remotes.get(0).name()).isEqualTo(REMOTE_NAME_UPSTREAM);
    assertThat(remotes.get(0).url()).isEqualTo(URL_SSH_DEVONFW);
  }
}
