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

  private static final String PROJECT_REPOSITORY = "repository";

  private static final String PROPERTIES_FILE = "test.properties";
  private static final String TEST_WORKSPACE = "test-workspace";
  private static final String TEST_BRANCH = "test-branch";
  public static final String TEST_REPO = "test-repo";
  public static final String TEST_GIT_REPO = "test-git-repo";

  /**
   * Creates default test properties for repository configuration.
   *
   * @return the {@link Properties} with default test values.
   */
  private Properties createDefaultProperties() {

    Properties properties = new Properties();
    properties.setProperty("path", TEST_REPO);
    properties.setProperty("workingsets", "test");
    properties.setProperty("workspace", TEST_WORKSPACE);
    properties.setProperty("git_url", "https://github.com/devonfw/" + TEST_GIT_REPO + ".git");
    properties.setProperty("git_branch", TEST_BRANCH);
    properties.setProperty("build_path", ".");
    properties.setProperty("build_cmd", "");
    properties.setProperty("active", "false");
    return properties;
  }

  /**
   * Saves the given properties to the test properties file.
   *
   * @param context the {@link IdeTestContext}.
   * @param properties the {@link Properties} to save.
   */
  private void saveProperties(IdeTestContext context, Properties properties) {

    Path repositoryTestProperties = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES).resolve(PROPERTIES_FILE);
    FileAccess fileAccess = context.getFileAccess();
    fileAccess.mkdirs(repositoryTestProperties.getParent());
    fileAccess.writeProperties(properties, repositoryTestProperties);
  }

  @Test
  public void testSetupSpecificRepository() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);
    // act
    rc.run();
    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  @Test
  public void testSetupAllRepositoriesInactive() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    // act
    rc.run();
    // assert
    assertThat(context).log().hasEntries(new IdeLogEntry(IdeLogLevel.STEP, "Start: Setup of repository test"),
        new IdeLogEntry(IdeLogLevel.INFO, "Skipping repository test because it is not active - use --force to setup all repositories ..."));
  }

  @Test
  public void testSetupSpecificRepositoryWithoutPath() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    properties.setProperty("path", "");
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString(PROPERTIES_FILE, context);
    // act
    rc.run();
    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve("test")).isDirectory();
  }

  @Test
  public void testSetupSpecificRepositoryFailsWithoutGitUrl() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    properties.setProperty("git_url", "");
    Path repositoryTestProperties = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES).resolve(PROPERTIES_FILE);
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString(PROPERTIES_FILE, context);
    // act
    rc.run();
    // assert
    assertThat(context).logAtError()
        .hasMessage("The properties file " + repositoryTestProperties + " must have a non-empty value for the required property git_url");
  }

  @Test
  public void testRunNoRepositoriesOrProjectsFolderFound() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    Path repositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
    context.getFileAccess().delete(repositoriesPath);
    // act
    rc.run();
    // assert
    assertThat(context).logAtWarning().hasMessage("Cannot find folder 'repositories' nor 'projects' in your settings.");
  }

  @Test
  public void testSetupSpecificRepositoryWithForceOption() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    context.getStartContext().setForceRepositories(true);
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);
    rc.repository.setValue(null, 0); //Overwrite the repository path to check if repositories should be forced

    // act
    rc.run();

    // assert
    assertThat(context).log().hasMessage("Setup of repository test is forced, hence proceeding ...");
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }
  @Test
  public void testSetupRepositoryWithMultipleWorkspaces() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    String workspace1 = "workspace1";
    String workspace2 = "workspace2";
    properties.setProperty("workspace", workspace1 + "," + workspace2);
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);

    // act
    rc.run();

    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace1).resolve(TEST_REPO)).isDirectory();
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace2).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  @Test
  public void testSetupRepositoryWithMultipleWorkspacesWithSpaces() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    String workspace1 = "workspace1";
    String workspace2 = "workspace2";
    String workspace3 = "workspace3";
    properties.setProperty("workspace", workspace1 + " , " + workspace2 + ", " + workspace3);
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);

    // act
    rc.run();

    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace1).resolve(TEST_REPO)).isDirectory();
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace2).resolve(TEST_REPO)).isDirectory();
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace3).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  @Test
  public void testSetupRepositoryWithEmptyWorkspaceDefaultsToMain() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    properties.setProperty("workspace", "");
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);

    // act
    rc.run();

    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }

  @Test
  public void testSetupRepositoryWithoutActiveProperty() {

    // arrange
    IdeTestContext context = newContext(PROJECT_REPOSITORY);
    Properties properties = createDefaultProperties();
    properties.remove("active"); // Remove the active property to test default behavior
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    saveProperties(context, properties);
    rc.repository.setValueAsString("test", context);
    // act
    rc.run();
    // assert - repository should be set up as if active=true was specified
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(TEST_WORKSPACE).resolve(TEST_REPO)).isDirectory();
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Setup of repository test'.");
  }
}
