package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.git.GitContextImplMock;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link CreateCommandlet}.
 */
class CreateCommandletTest extends AbstractIdeContextTest {

  private static final String NEW_PROJECT_NAME = "newProject";
  private IdeTestContext context;

  /**
   * Create the project basic context, delete new project if it exists.
   */
  @BeforeEach
  void setup() {
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    if (Files.exists(newProjectPath)) {
      context.getFileAccess().delete(newProjectPath);
    }
    this.context = context;
  }

  /**
   * Reset the current version back to SNAPSHOT so further tests don't fail
   */
  @AfterEach
  void tearDown() {
    IdeVersion.setSnapshotVersionForTesting();
  }

  @Test
  void testCreateCommandletRun() {

    // arrange
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);
    // act
    cc.run();
    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(context.getIdeHome()).isEqualTo(newProjectPath);
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN)).exists();
    // the settings have to be cloned into the new project and not into the project the create command was started from
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SETTINGS).resolve("ide.properties")).exists();
    assertThat(context.getIdeRoot().resolve("_ide/tmp/projects").resolve(NEW_PROJECT_NAME)).doesNotExist();
  }

  @Test
  void testIdeVersionTooOldOnProjectCreation(@TempDir Path tempDir) throws Exception {
    // arrange
    String ideMinVersion = "2026.01.001";
    IdeVersion.setMockVersionForTesting("2024.01.001");
    String ideCurrentVersion = IdeVersion.getVersionString();
    String errorMessage = String.format("""
        Your version of IDEasy is currently %s
        However, this is too old as your project requires at latest version %s
        Please run the following command to update to the latest version of IDEasy and fix the problem:
        ide upgrade""", ideCurrentVersion, ideMinVersion);

    // IDE_MIN_VERSION must exist in the settings that are cloned into the new project
    Path settingsRepo = tempDir.resolve("settings");
    Files.createDirectories(settingsRepo);
    Files.writeString(settingsRepo.resolve("ide.properties"), "IDE_MIN_VERSION=" + ideMinVersion + System.lineSeparator());

    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, settingsRepo);
    context.setGitContext(gitContextImplMock);

    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act & assert
    assertThatThrownBy(cc::run).isInstanceOf(CliException.class).hasMessage(errorMessage);
  }

  @Test
  void testIdeVersionTooOldForExistingProject() {
    // arrange
    String path = "project/workspaces/foo-test";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentVariables variables = context.getVariables();
    String ideMinVersion = "2026.01.001"; // mocks the minimum required version
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_MIN_VERSION", ideMinVersion);
    IdeVersion.setMockVersionForTesting("2024.01.001"); // mocks the current version (instead of using SNAPSHOT)
    String ideCurrentVersion = IdeVersion.getVersionString();
    CliArguments args = new CliArguments("status");
    String warningMessage = String.format("Your version of IDEasy is currently %s\n"
        + "However, this is too old as your project requires at latest version %s", ideCurrentVersion, ideMinVersion);
    String interactionMessage = "Please run the following command to update to the latest version of IDEasy and fix the problem:\n"
        + "ide upgrade";

    // act
    context.run(args);

    // assert
    assertThat(context).logAtWarning().hasMessage(warningMessage);
    assertThat(context).logAtInteraction().hasMessage(interactionMessage);
  }

  @Test
  void testIdeVersionOk() {
    // arrange
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);
    EnvironmentVariables variables = context.getVariables();
    String ideVersion = IdeVersion.getVersionIdentifier().toString();
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_MIN_VERSION", ideVersion);
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);

    // act
    cc.run();

    // assert
    assertThat(newProjectPath).exists();
    assertThat(context.getIdeHome()).isEqualTo(newProjectPath);
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN)).exists();
    assertThat(context.getFileAccess().readFileContent(newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION)))
        .isEqualTo(IdeVersion.getVersionString());
    assertThat(context).log()
        .hasNoMessageContaining("However, this is too old as your project requires at latest version")
        .hasNoMessageContaining("run the following command to update to the latest version of IDEasy")
        .hasMessageContaining("Successfully created new project '" + NEW_PROJECT_NAME + "'.");
  }

  @Test
  void testWelcomeMessageDisplayed() {

    // arrange - create a new project
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, TEST_RESOURCES.resolve("settings"));

    context.setGitContext(gitContextImplMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act - run the create command
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(context).logAtInfo().hasMessageContaining("Welcome to your new IDEasy project!");
  }

  @Test
  void testProjectWithInvalidRepositoryNotCreated() {

    // arrange - create a new project that is invalid (does not contain ide.properties file)
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, TEST_RESOURCES.resolve("pypi"));

    context.setGitContext(gitContextImplMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act - run the create command
    assertThatThrownBy(cc::run)
        .isInstanceOf(CliException.class)
        .hasMessageContaining(
            "Settings repository integrity check failed: "
                + "The given git repository URL does not point to a valid settings or code-settings repository. Please verify and try again.");

    // assert - if "ide create" fails then no project shall be created at all
    assertThat(context.getIdeRoot().resolve(NEW_PROJECT_NAME)).doesNotExist();
    assertThat(context.getTempPath().resolve(IdeContext.FOLDER_PROJECTS).resolve(NEW_PROJECT_NAME)).doesNotExist();
  }

  @Test
  void testCreateWithCodeSettingsRepository() {

    // arrange - a combined code and settings repository has the settings in a top-level "settings" folder
    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, TEST_RESOURCES.resolve("code-settings"));
    context.setGitContext(gitContextImplMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue("https://github.com/devonfw/code-settings-repo.git");
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert - the repository is placed into the workspace and IDE_HOME/settings is a symlink to its settings folder
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path codePath = newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN).resolve("code-settings-repo");
    assertThat(codePath.resolve("pom.xml")).exists();
    assertThat(codePath.resolve(IdeContext.FOLDER_SETTINGS).resolve("ide.properties")).exists();
    Path settingsLink = newProjectPath.resolve(IdeContext.FOLDER_SETTINGS);
    assertThat(settingsLink).isSymbolicLink();
    assertThat(settingsLink.resolve("ide.properties")).exists();
  }

  @Test
  void testCreateWithInvalidRepositoryContinuesInForceMode() {

    // arrange - force mode lets the user decide to continue even though the health check failed
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, TEST_RESOURCES.resolve("pypi"));
    context.setGitContext(gitContextImplMock);
    context.getStartContext().setForceMode(true);
    context.setAnswers("yes");
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);
    cc.skipRepositories.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(context).logAtWarning()
        .hasMessageContaining("does not point to a valid settings or code-settings repository");
  }

  @Test
  void testCreateWithDashPlaceholderAsCliArgument() {
    // arrange - see https://github.com/devonfw/IDEasy/issues/2106
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context, TEST_RESOURCES.resolve("settings"));
    context.setGitContext(gitContextImplMock);
    CliArguments args = new CliArguments("create", NEW_PROJECT_NAME, "-", "--skip-tools");

    // act
    int result = context.run(args);

    // assert
    assertThat(result).isEqualTo(0);
    assertThat(context).logAtError().hasNoMessageContaining("not found for commandlet");
    assertThat(context).logAtInfo()
        .hasMessageContaining("'-' was found for the repository, the default settings repository");
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
  }
}
