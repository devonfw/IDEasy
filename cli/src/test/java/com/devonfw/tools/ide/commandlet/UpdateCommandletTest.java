package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.commandlet.update.UpdateCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UpdateCommandlet}.
 */
@WireMockTest
class UpdateCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_UPDATE = "update";
  private static final String SUCCESS_UPDATE_SETTINGS = "Successfully ended step 'Update settings repository'.";
  private static final String SUCCESS_INSTALL_OR_UPDATE_SOFTWARE = "Install or update software";

  @Test
  void testRunPullSettingsAndUpdateSoftware() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    Path extraPath = context.getSoftwareExtraPath();
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    Java java = context.getCommandletManager().getCommandlet(Java.class);
    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);
    // act
    uc.run();
    java.arguments.addValue("--version");
    java.run();
    mvn.arguments.addValue("-v");
    mvn.run();
    Path javaClient = extraPath.resolve("java/client/bin/java");
    context.newProcess().executable(javaClient).addArg("--client").run();
    Path javaProcessEngine = extraPath.resolve("java/process-engine/bin/java");
    context.newProcess().executable(javaProcessEngine).addArg("--process-engine").run();
    Path mvnM4 = extraPath.resolve("mvn/m4/bin/mvn");
    context.newProcess().executable(mvnM4).addArg("--m4").run();

    // assert
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context).log().hasNoMessageContaining(" ended with failure");
    assertThat(context.getConfPath()).exists();
    assertThat(context.getSoftwarePath().resolve("java")).exists();
    assertThat(context.getSoftwarePath().resolve("mvn")).exists();
    assertThat(context).logAtInfo().hasMessage("java 17.0.6 --version");
    assertThat(context).logAtInfo().hasMessage("mvn 3.2.1 -v");
    assertThat(context).logAtInfo().hasMessage("java 11.0.27_6 --client");
    assertThat(context).logAtInfo().hasMessage("java 21.0.9_10 --process-engine");
    assertThat(context).logAtInfo().hasMessage("mvn 4.0.0-rc-5 --m4");
    assertThat(javaClient).exists();
    assertThat(javaProcessEngine).exists();
    assertThat(mvnM4).exists();
  }

  /**
   * The project has <code>IDE_TOOLS=java, mvn</code> and <code>mvn</code> depends on <code>java</code>, so <code>java</code> is both a top-level tool and a
   * dependency (of both the regular and the extra <code>mvn</code>). Before <a href="https://github.com/devonfw/IDEasy/issues/945">#945</a> the CVE check for
   * <code>java</code> ran once per reference, i.e. several times. Now it must run only once per unique tool/version per process.
   */
  /**
   * An extra installation of a tool that has already been installed (and is thus tracked) during the same process must NOT be skipped: it is linked at its
   * own extra tool path even though the tracked version is already present in the software repository.
   */
  @Test
  void testRunPullSettingsDoesNotSkipExtraInstallationOfTrackedTool() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    // the project version of java is 17.0.6 (JAVA_VERSION in ide.properties); the extra installation uses the very same version and edition
    String extraTools = """
        {
          "java": {
            "pinned": {
              "version": "17.0.6"
            }
          }
        }
        """;
    context.getFileAccess().writeFileContent(extraTools, context.getSettingsPath().resolve("ide-extra-tools.json"));
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);

    // act
    uc.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install extra version(s) of java'.");
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
    assertThat(context.getSoftwareExtraPath().resolve("java/pinned")).exists();
  }

  /**
   * <code>java</code> 17.0.6 has a CVE and the only safe version available for the <code>java</code> edition is 21.0.9_10, so the CVE check bumps
   * <code>java</code> to 21.0.9_10 when it is installed. Because <code>java</code> is both a top-level tool and a dependency of <code>mvn</code>, the CVE check
   * must be offered only once per process: the skipped (dependency) install must be matched on the originally requested (pre-CVE) version, not on the version
   * the CVE check resolved to. See <a href="https://github.com/devonfw/IDEasy/issues/945">#945</a>.
   */
  @Test
  void testRunPullSettingsDoesNotReAskCveChoiceForSharedDependencyWhenVersionIsBumped() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    // give the shared 'java' tool a CVE for 17.0.6; the only safe version available for the 'java' edition is 21.0.9_10, so the CVE check suggests 'latest'
    String securityJson = """
        {
          "issues": [
            {
              "id": "CVE-TEST",
              "severity": 9.0,
              "versions": [
                "[17.0.6]"
              ]
            }
          ]
        }
        """;
    context.getFileAccess().writeFileContent(securityJson, context.getUrlsPath().resolve("java").resolve("security.json"));
    // answer 'latest' to the single CVE question for 'java' (current + nearest + latest are all 21.0.9_10)
    context.setAnswers("latest");
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);

    // act
    uc.run();

    // assert
    long cveQuestionCount = context.getTestStartContext().getEntries().stream()
        .filter(e -> e.message().contains("Which version do you want to install?"))
        .count();
    assertThat(cveQuestionCount)
        .as("the CVE choice must be asked exactly once per process, not once per reference to 'java'").isEqualTo(1);
    assertThat(context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve("21.0.9_10"))
        .as("the CVE check must have bumped 'java' to the safe version 21.0.9_10").exists();
  }

  @Test
  void testRunPullSettingsDoesNotRunCveCheckTwiceForSharedDependency() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);

    // act
    uc.run();

    // assert
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
    long cveCheckCount = context.getTestStartContext().getEntries().stream()
        .filter(e -> e.level() == IdeLogLevel.SUCCESS)
        .filter(e -> e.message().contains("No CVEs found for version 17.0.6 of tool java"))
        .count();
    assertThat(cveCheckCount).as("CVE check for shared dependency 'java' must run exactly once per process").isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "eclipse", "intellij", "eclipse,intellij", "intellij , vscode", "eclipse, intellij,vscode" })
  void testIdeUpdateCreatesStartScripts(String createStartScripts) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    EnvironmentVariables settings = context.getVariables().getByType(EnvironmentVariablesType.SETTINGS);
    settings.set(IdeVariables.CREATE_START_SCRIPTS.getName(), createStartScripts);
    settings.save();
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    String[] activeIdes = Arrays.stream(createStartScripts.split(",")).map(String::trim).filter(ide -> !ide.isEmpty()).toArray(String[]::new);
    // act
    uc.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    verifyStartScriptsForAllWorkspacesAndAllIdes(context, activeIdes);
  }

  @Test
  void testRunTemplatesNotFound() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    deleteTemplatesFolder(context);

    // act
    uc.run();

    // assert
    assertThat(context).logAtWarning().hasEntries("Templates folder is missing in settings repository.");
  }

  private void deleteTemplatesFolder(IdeContext context) {

    Path templates = context.getSettingsPath().resolve(IdeContext.FOLDER_TEMPLATES);
    context.getFileAccess().delete(templates);
  }

  /**
   * Tests if a sub step (installation of software) of update failed, the overall update process will not fail too.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/628">#628</a> for reference.
   */
  @Test
  void testRunUpdateSoftwareDoesNotFailOnFailedSoftwareInstallations() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    Path javaRepository = context.getToolRepositoryPath().resolve("default").resolve("java");
    context.getFileAccess().delete(javaRepository);
    Path javaDownload = context.getIdeRoot().resolve(IdeContext.FOLDER_REPOSITORY).resolve("java");
    context.getFileAccess().delete(javaDownload);
    UpdateCommandlet update = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);

    // act
    update.run();

    // assert
    assertThat(context).logAtError().hasMessage("Step 'Install java' ended with failure.");
    assertThat(context).logAtError().hasMessage("Step 'Install mvn' ended with failure.");
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
  }

  /**
   * Tests if the update process can continue even when the settings were deleted. This test is highly mocked using Wiremock and other classes f.e.
   * {@link com.devonfw.tools.ide.git.GitContextMock} is responsible for this test to succeed and not use our real repository.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testRunUpdateSoftwareDoesNotFailWhenSettingsPathIsDeleted(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE, wireMockRuntimeInfo);
    Path settingsPath = context.getSettingsPath();
    context.getFileAccess().delete(settingsPath);
    UpdateCommandlet update = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    context.setAnswers("-");

    // act
    update.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
  }

  /**
   * Tests that a settings folder that exists but is not a git repository is backed up and cloned from scratch after the user confirmed.
   */
  @Test
  void testRunUpdateWithBrokenSettingsFolder() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    Path settingsPath = context.getSettingsPath();
    // remove the '.git' folder so the settings are present but broken
    context.getFileAccess().delete(settingsPath.resolve(GitContext.GIT_FOLDER));
    UpdateCommandlet update = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    // first answer confirms the backup of the broken settings, second answer picks the default settings repository
    context.setAnswers("yes", "-");

    // act
    update.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_BACKUPS)).exists();
    assertThat(settingsPath.resolve(GitContext.GIT_FOLDER)).exists();
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
  }

  /**
   * Tests that a failing "git pull" (e.g. due to an error of a custom git server) only fails the settings step while the software is still installed.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/pull/2335">#2335</a> for reference.
   */
  @Test
  void testRunUpdateContinuesWhenPullFails() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    context.setGitContext(new GitContextMock(context) {
      @Override
      public void pull(Path repository) {

        throw new IllegalStateException("git pull failed due to an error of the custom git server");
      }
    });
    UpdateCommandlet update = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);

    // act
    update.run();

    // assert
    assertThat(context).logAtError().hasMessage("Step 'Applying settings' ended with failure.");
    assertThat(context).log().hasNoMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context).logAtSuccess().hasMessageContaining(SUCCESS_INSTALL_OR_UPDATE_SOFTWARE);
  }
}
