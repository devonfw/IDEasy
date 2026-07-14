package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
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
  void testSecretPlaceholderInTemplateIsReplaced() {
    // arrange
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-secret-placeholder"));
    context.setGitContext(gitContextImplMock);
    context.setAnswers("test-secret-value");
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedSettings = newProjectPath.resolve("conf").resolve("claude").resolve("settings.json");
    assertThat(generatedSettings).exists();
    String generatedContent = context.getFileAccess().readFileContent(generatedSettings);
    // The generated file should contain the secret value and no longer the placeholder
    assertThat(generatedContent).contains("test-secret-value");
    assertThat(generatedContent).doesNotContain("$[secret:ai-api-key]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-secret-placeholder")
        .resolve("templates").resolve("conf").resolve("claude").resolve("settings.json");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(sourceContent).contains("$[secret:ai-api-key]");
    assertThat(sourceContent).doesNotContain("test-secret-value");
  }

  @Test
  void testPlaceholderLikeTextInSecretIsNotProcessed() {
    // arrange — the entered secret itself contains placeholder-like text
    String secretWithPlaceholderText = "prefix-$[secret:not-a-template-placeholder]-suffix";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-placeholder-like-secret"));
    context.setGitContext(gitContextImplMock);
    // Only one answer — if the embedded placeholder-like text were rescanned, a second prompt would fail
    context.setAnswers(secretWithPlaceholderText);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedFile = newProjectPath.resolve("conf").resolve("example").resolve("config.txt");
    assertThat(generatedFile).exists();
    String generatedContent = context.getFileAccess().readFileContent(generatedFile);
    // The generated file should contain the secret value verbatim, including its placeholder-like text
    assertThat(generatedContent).isEqualTo("value=" + secretWithPlaceholderText);
    // The embedded placeholder-like text must remain literal (it came from the entered value, not the template)
    assertThat(generatedContent).contains("$[secret:not-a-template-placeholder]");
    // The original template placeholder must be gone
    assertThat(generatedContent).doesNotContain("$[secret:outer-secret]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-placeholder-like-secret")
        .resolve("templates").resolve("conf").resolve("example").resolve("config.txt");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(sourceContent).contains("$[secret:outer-secret]");
    assertThat(sourceContent).doesNotContain(secretWithPlaceholderText);
  }

  @Test
  void testSecretValueNotLoggedByProductionCode() {
    // arrange
    String secretWithMarker = "TOP-SECRET-LOG-MARKER-7f3a";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-secret-placeholder"));
    context.setGitContext(gitContextImplMock);
    context.setAnswers(secretWithMarker);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedSettings = newProjectPath.resolve("conf").resolve("claude").resolve("settings.json");
    // The generated file should contain the secret
    assertThat(generatedSettings).exists();
    assertThat(context.getFileAccess().readFileContent(generatedSettings)).contains(secretWithMarker);

    // No production log level (DEBUG, INFO, WARNING, ERROR, SUCCESS) may contain the secret
    assertThat(context).logAtDebug().hasNoMessageContaining(secretWithMarker);
    assertThat(context).logAtInfo().hasNoMessageContaining(secretWithMarker);
    assertThat(context).logAtWarning().hasNoMessageContaining(secretWithMarker);
    assertThat(context).logAtError().hasNoMessageContaining(secretWithMarker);
    assertThat(context).logAtSuccess().hasNoMessageContaining(secretWithMarker);
  }

  @Test
  void testExistingLocalConfFileIsNotOverwrittenByTemplateWithSecret() {
    // arrange — the destination conf file already exists with local content
    String existingLocalSecret = "EXISTING-LOCAL-SECRET-91c4";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-secret-placeholder"));
    context.setGitContext(gitContextImplMock);
    // Only answer the non-empty-directory confirmation.
    // No secret answer is queued, so an unexpected secret prompt would fail.
    context.setAnswers("yes");
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // Pre-create the destination file before running create, so it looks like an existing local config
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path preExistingFile = newProjectPath.resolve("conf").resolve("claude").resolve("settings.json");
    context.getFileAccess().mkdirs(preExistingFile.getParent());
    context.getFileAccess().writeFileContent(existingLocalSecret, preExistingFile, true);

    // act
    cc.run();

    // assert — the destination file must still contain the original local content
    String finalContent = context.getFileAccess().readFileContent(preExistingFile);
    assertThat(finalContent).isEqualTo(existingLocalSecret);
    assertThat(finalContent).doesNotContain("$[secret:ai-api-key]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-secret-placeholder")
        .resolve("templates").resolve("conf").resolve("claude").resolve("settings.json");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(sourceContent).contains("$[secret:ai-api-key]");
    assertThat(sourceContent).doesNotContain(existingLocalSecret);

    // No production log level may contain the existing local secret
    assertThat(context).logAtDebug().hasNoMessageContaining(existingLocalSecret);
    assertThat(context).logAtInfo().hasNoMessageContaining(existingLocalSecret);
    assertThat(context).logAtWarning().hasNoMessageContaining(existingLocalSecret);
    assertThat(context).logAtError().hasNoMessageContaining(existingLocalSecret);
    assertThat(context).logAtSuccess().hasNoMessageContaining(existingLocalSecret);

    // Verify the skip log was emitted (proves the file was not overwritten)
    assertThat(context).logAtDebug()
        .hasMessageContaining("already exists - skipping to copy from");
  }

  @Test
  void testRepeatedSamePlaceholderIsReplacedTwice() {
    // arrange — same placeholder appears twice; only one answer is queued
    String repeatedSecret = "repeated-test-secret";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-repeated-secret"));
    context.setGitContext(gitContextImplMock);
    context.setAnswers(repeatedSecret);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedFile = newProjectPath.resolve("conf").resolve("example").resolve("config.json");
    assertThat(generatedFile).exists();
    String generatedContent = context.getFileAccess().readFileContent(generatedFile);
    // Both occurrences of the placeholder should be replaced with the single answer
    assertThat(countOccurrences(generatedContent, repeatedSecret)).isEqualTo(2);
    assertThat(generatedContent).doesNotContain("$[secret:ai-api-key]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-repeated-secret")
        .resolve("templates").resolve("conf").resolve("example").resolve("config.json");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(countOccurrences(sourceContent, "$[secret:ai-api-key]")).isEqualTo(2);
    assertThat(sourceContent).doesNotContain(repeatedSecret);
  }

  private static int countOccurrences(String text, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = text.indexOf(needle, idx)) != -1) {
      count++;
      idx += needle.length();
    }
    return count;
  }

  @Test
  void testDistinctSecretPlaceholdersAreReplaced() {
    // arrange — two different secret placeholders; two answers in encounter order
    String aiSecret = "test-ai-secret";
    String dbSecret = "test-database-secret";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-distinct-secrets"));
    context.setGitContext(gitContextImplMock);
    context.setAnswers(aiSecret, dbSecret);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedFile = newProjectPath.resolve("conf").resolve("example").resolve("config.json");
    assertThat(generatedFile).exists();
    String generatedContent = context.getFileAccess().readFileContent(generatedFile);
    // Both placeholders should be replaced with their respective values
    assertThat(generatedContent).contains("\"api\": \"" + aiSecret + "\"");
    assertThat(generatedContent).contains("\"database\": \"" + dbSecret + "\"");
    // Values must not be swapped
    assertThat(generatedContent).doesNotContain("\"api\": \"" + dbSecret + "\"");
    assertThat(generatedContent).doesNotContain("\"database\": \"" + aiSecret + "\"");
    // No unresolved placeholders should remain
    assertThat(generatedContent).doesNotContain("$[secret:ai-api-key]");
    assertThat(generatedContent).doesNotContain("$[secret:database-password]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-distinct-secrets")
        .resolve("templates").resolve("conf").resolve("example").resolve("config.json");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(sourceContent).contains("$[secret:ai-api-key]");
    assertThat(sourceContent).contains("$[secret:database-password]");
    assertThat(sourceContent).doesNotContain(aiSecret);
    assertThat(sourceContent).doesNotContain(dbSecret);
  }

  @Test
  void testTemplateCopyWithoutPlaceholder() {
    // arrange — plain template with no secret placeholder, copied via original FileAccess.copy behavior
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-plain-conf"));
    context.setGitContext(gitContextImplMock);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedFile = newProjectPath.resolve("conf").resolve("example").resolve("config.txt");
    assertThat(generatedFile).exists();
    assertThat(generatedFile).hasContent("plain configuration content");
    // No accidentally nested path (would indicate wrong FileAccess.copy target)
    Path nestedPath = generatedFile.resolve("config.txt");
    assertThat(nestedPath).doesNotExist();
  }

  @Test
  void testSecretPlaceholderWithSpecialReplacementCharacters() {
    // arrange — secret value contains $ and \ which are special in Matcher replacements
    String specialSecret = "test$ecret\\value";
    GitContextImplMock gitContextImplMock = new GitContextImplMock(context,
        TEST_RESOURCES.resolve("settings-with-special-secret"));
    context.setGitContext(gitContextImplMock);
    context.setAnswers(specialSecret);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    cc.settingsRepo.setValue(IdeContext.DEFAULT_SETTINGS_REPO_URL);
    cc.skipTools.setValue(true);

    // act
    cc.run();

    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    Path generatedSettings = newProjectPath.resolve("conf").resolve("claude").resolve("settings.json");
    assertThat(generatedSettings).exists();
    String generatedContent = context.getFileAccess().readFileContent(generatedSettings);
    // The generated file should contain the exact literal secret value including $ and \
    assertThat(generatedContent).contains(specialSecret);
    assertThat(generatedContent).doesNotContain("$[secret:ai-api-key]");

    // The source template must remain unchanged
    Path sourceTemplate = TEST_RESOURCES.resolve("settings-with-special-secret")
        .resolve("templates").resolve("conf").resolve("claude").resolve("settings.json");
    String sourceContent = context.getFileAccess().readFileContent(sourceTemplate);
    assertThat(sourceContent).contains("$[secret:ai-api-key]");
    assertThat(sourceContent).doesNotContain(specialSecret);
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
        .hasMessageContaining("'-' was found for settings repository, the default settings repository");
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
  }
}
