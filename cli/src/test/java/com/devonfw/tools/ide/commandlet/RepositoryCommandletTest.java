package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

public class RepositoryCommandletTest extends AbstractIdeContextTest {

  private static final String PROPERTIES_FILE = "test.properties";
  private static final String TEST_WORKSPACE = "test-workspace";
  private static final String TEST_BRANCH = "test-branch";
  public static final String TEST_REPO = "test-repo";
  public static final String TEST_GIT_REPO = "test-git-repo";

  /** {@link Properties} of the repository to test. */
  private final Properties properties;

  private final IdeTestContext context;

  private final Path repositoryTestProperties;

  /**
   * The constructor.
   */
  public RepositoryCommandletTest() {
    super();
    // actually this should be done in JUnit BeforeAll (setup)
    // the best way is to avoid such state and just use local variables and private methods in test methods.
    this.properties = new Properties();
    this.properties.setProperty("path", TEST_REPO);
    this.properties.setProperty("workingsets", "test");
    this.properties.setProperty("workspace", TEST_WORKSPACE);
    this.properties.setProperty("git_url", "https://github.com/devonfw/" + TEST_GIT_REPO + ".git");
    this.properties.setProperty("git_branch", TEST_BRANCH);
    this.properties.setProperty("build_path", ".");
    this.properties.setProperty("build_cmd", "");
    this.properties.setProperty("active", "false");
    this.context = newContext(PROJECT_BASIC);
    this.repositoryTestProperties = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES).resolve(PROPERTIES_FILE);
  }

  @Test
  public void testSetupSpecificRepository() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties();
    rc.repository.setValueAsString("test", this.context);
    // act
    rc.run();
    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve(TEST_REPO)).isDirectory();
    assertThat(this.context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  @Test
  public void testSetupAllRepositoriesInactive() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties();
    // act
    rc.run();
    // assert
    assertThat(this.context).log().hasEntries(new IdeLogEntry(IdeLogLevel.STEP, "Start: Setup of repository test"),
        new IdeLogEntry(IdeLogLevel.INFO, "Skipping repository test because it is not active - use --force to setup all repositories ..."));
  }

  @Test
  public void testSetupSpecificRepositoryWithoutPath() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    this.properties.setProperty("path", "");
    saveProperties();
    rc.repository.setValueAsString(PROPERTIES_FILE, this.context);
    // act
    rc.run();
    // assert
    assertThat(this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve("test")).isDirectory();
  }

  @Test
  public void testSetupSpecificRepositoryFailsWithoutGitUrl() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    this.properties.setProperty("git_url", "");
    saveProperties();
    rc.repository.setValueAsString(PROPERTIES_FILE, this.context);
    // act
    rc.run();
    // assert
    assertThat(this.context).logAtError()
        .hasMessage("The properties file " + this.repositoryTestProperties + " must have a non-empty value for the required property git_url");
  }

  @Test
  public void testRunNoRepositoriesOrProjectsFolderFound() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    Path repositoriesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
    this.context.getFileAccess().delete(repositoriesPath);
    // act
    rc.run();
    // assert
    assertThat(this.context).logAtWarning().hasMessage("Cannot find folder 'repositories' nor 'projects' in your settings.");
  }

  @Test
  public void testSetupSpecificRepositoryWithForceOption() {

    // arrange
    this.context.setForceRepositories(true);
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties();
    rc.repository.setValueAsString("test", this.context);
    rc.repository.setValue(null, 0); //Overwrite the repository path to check if repositories should be forced

    // act
    rc.run();

    // assert
    assertThat(this.context).log().hasMessage("Setup of repository test is forced, hence proceeding ...");
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve(TEST_REPO)).isDirectory();
    assertThat(this.context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  private void saveProperties() {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(this.repositoryTestProperties.getParent());
    fileAccess.writeProperties(this.properties, this.repositoryTestProperties);
  }
}
