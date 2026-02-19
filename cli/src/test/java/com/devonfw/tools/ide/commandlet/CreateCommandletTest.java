package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextGitMock;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.git.GitContextImplMock;
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
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://some-code-repository", "ssh://some-settings-repository" })
  void testWarningWhenRepoDoesNotMeetNamingConvention(String invalidRepo, @TempDir Path tempDir) {
    // arrange
    ProcessContextGitMock gitMock = new ProcessContextGitMock(context, tempDir);
    context.setProcessContext(gitMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.codeRepositoryFlag.setValue(!invalidRepo.contains("code")); // raise conflict
    cc.settingsRepo.setValue(invalidRepo);
    cc.skipTools.setValue(true);
    context.setAnswers("yes");
    // act
    cc.run();
    // assert
    assertThat(context).logAtInteraction().hasMessageContaining("Do you really want to create the project?");
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(context.getIdeHome()).isEqualTo(newProjectPath);
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN)).exists();
  }

  @Test
  void testWarningWhenCodeRepoUsingDefaultMark(@TempDir Path tempDir) {
    String invalidCodeRepo = "-";
    // arrange
    ProcessContextGitMock gitMock = new ProcessContextGitMock(context, tempDir);
    context.setProcessContext(gitMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(invalidCodeRepo);
    cc.codeRepositoryFlag.setValue(true);
    cc.skipTools.setValue(true);
    context.setAnswers("https://some-code-repository");
    // act
    cc.run();
    // assert
    assertThat(context).logAtWarning().hasMessageContaining("'-' is found after '--code'. This is invalid.");
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(context.getIdeHome()).isEqualTo(newProjectPath);
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN)).exists();
  }

  @Test
  void testIdeVersionTooOldOnProjectCreation() {
    // arrange
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);
    EnvironmentVariables variables = context.getVariables();
    String ideMinVersion = "2026.01.001"; // mocks the minimum required version
    IdeVersion.setMockVersionForTesting("2024.01.001"); // mocks the current version (instead of using SNAPSHOT)
    String ideCurrentVersion = IdeVersion.getVersionString();
    String errorMessage = String.format("Your version of IDEasy is currently %s\n"
        + "However, this is too old as your project requires at latest version %s\n"
        + "Please run the following command to update to the latest version of IDEasy and fix the problem:\n"
        + "ide upgrade", ideCurrentVersion, ideMinVersion);
    // act
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_MIN_VERSION", ideMinVersion);
    // assert
    assertThatThrownBy(() -> cc.run()).hasMessage(errorMessage);
    IdeVersion.setSnapshotVersionForTesting(); // reset current version back to SNAPSHOT so further tests don't fail
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
    CliArguments args = new CliArguments();
    String warningMessage = String.format("Your version of IDEasy is currently %s\n"
        + "However, this is too old as your project requires at latest version %s\n"
        + "Please run the following command to update to the latest version of IDEasy and fix the problem:\n"
        + "ide upgrade", ideCurrentVersion, ideMinVersion);
    // act
    context.run(args);
    IdeVersion.setSnapshotVersionForTesting(); // reset current version back to SNAPSHOT so further tests don't fail
    // assert
    assertThat(context).logAtWarning().hasMessage(warningMessage);
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
    String errorMessage = String.format("Your version of IDEasy is currently %s\n"
        + "However, this is too old as your project requires at latest version %s\n"
        + "Please run the following command to update to the latest version of IDEasy and fix the problem:\n"
        + "ide upgrade", ideVersion, ideVersion);
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
    assertThat(context).logAtError().hasNoMessageContaining(errorMessage);
    assertThat(context).logAtWarning()
        .hasNoMessageContaining("However, this is too old as your project requires at latest version");
    assertThat(context).logAtSuccess()
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
}
