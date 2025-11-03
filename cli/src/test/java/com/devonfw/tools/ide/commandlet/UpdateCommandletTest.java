package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UpdateCommandlet}.
 */
@WireMockTest
class UpdateCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_UPDATE = "update";
  private static final String SUCCESS_UPDATE_SETTINGS = "Successfully ended step 'update (pull) settings repository'.";
  private static final String SUCCESS_INSTALL_OR_UPDATE_SOFTWARE = "Install or update software";

  @Test
  public void testRunPullSettingsAndUpdateSoftware() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    // act
    uc.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage(SUCCESS_UPDATE_SETTINGS);
    assertThat(context.getConfPath()).exists();
    assertThat(context.getSoftwarePath().resolve("java")).exists();
    assertThat(context.getSoftwarePath().resolve("mvn")).exists();
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "eclipse", "intellij", "eclipse,intellij", "intellij , vscode", "eclipse, intellij,vscode" })
  public void testIdeUpdateCreatesStartScripts(String createStartScripts) {

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
  public void testRunTemplatesNotFound() throws IOException {

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
  public void testRunUpdateSoftwareDoesNotFailOnFailedSoftwareInstallations() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    Path javaRepository = context.getToolRepositoryPath().resolve("default").resolve("java");
    context.getFileAccess().delete(javaRepository);
    Path javaDownload = context.getIdeRoot().resolve("repository").resolve("java");
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

  @Test
  public void testRunUpdateSoftwareDoesNotFailWhenSettingPathIsDeleted(WireMockRuntimeInfo wireMockRuntimeInfo) {

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
}
