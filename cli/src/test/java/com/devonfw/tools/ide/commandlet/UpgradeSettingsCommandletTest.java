package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.custom.CustomToolJson;
import com.devonfw.tools.ide.tool.custom.CustomToolsJson;
import com.devonfw.tools.ide.tool.custom.CustomToolsJsonMapper;

/**
 * Integration test of {@link UpgradeSettingsCommandlet} .
 */
public class UpgradeSettingsCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_UPGRADE_SETTINGS = "upgrade-settings";
  private final IdeTestContext context = newContext(PROJECT_UPGRADE_SETTINGS);
  private static final Path UPGRADE_SETTINGS_PATH = TEST_PROJECTS_COPY.resolve(PROJECT_UPGRADE_SETTINGS).resolve("project");

  /**
   * Test of {@link UpgradeSettingsCommandlet}.
   *
   * @throws Exception on error.
   */
  @Test
  public void testUpdateSettings() throws Exception {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    // assert
    verifyUpdateLegacyFolders();
    verifyUpdateProperties();
    verifyUpdateWorkspaceTemplates();
  }

  /**
   * @throws Exception on error.
   */
  private void verifyUpdateProperties() throws Exception {

    assertThat(UPGRADE_SETTINGS_PATH.resolve("home/.ide/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/ide.properties")).exists().content().contains("INTELLIJ_EDITION=ultimate")
        .doesNotContain("INTELLIJ_EDITION_TYPE").contains("IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED=false");
    assertThat(UPGRADE_SETTINGS_PATH.resolve("workspaces/main/ide.properties")).exists();
    //assert that file content was changed
    assertThat(UPGRADE_SETTINGS_PATH.resolve("conf/ide.properties")).exists().content().contains("MVN_VERSION=test");

    // devon.properties have been deleted (moved to backup)
    assertThat(UPGRADE_SETTINGS_PATH.resolve("home/devon.properties")).doesNotExist();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/devon.properties")).doesNotExist();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("workspaces/main/devon.properties")).doesNotExist();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("conf/devon.properties")).doesNotExist();
    verifyCustomToolsJson();
  }

  private void verifyCustomToolsJson() throws Exception {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    // assert

    Path customToolsJsonFile = UPGRADE_SETTINGS_PATH.resolve("settings").resolve(IdeContext.FILE_CUSTOM_TOOLS);
    // assert that ide-custom-tools.json exists
    assertThat(customToolsJsonFile).exists();
    CustomToolsJson customToolsJson = CustomToolsJsonMapper.loadJson(customToolsJsonFile);
    //assert that ide-custom-tools.json has the correct content
    assertThat(customToolsJson.url()).isEqualTo("https://host.tld/projects/my-project");
    assertThat(customToolsJson.tools()).containsExactly(new CustomToolJson("jboss-eap", "7.1.4.GA", true, true, null),
        new CustomToolJson("firefox", "70.0.1", false, false, null));
  }

  private void verifyUpdateLegacyFolders() {
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/repositories/IDEasy.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/templates/conf/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/templates/conf/mvn/settings.xml")).exists();
  }

  private void verifyUpdateWorkspaceTemplates() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    //assert
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/workspace/testVariableSyntax.txt")).exists().content().contains("$[IDE_HOME]").contains("$[MVN_VERSION]")
        .contains("$[IDE_HOME]/conf/mvn/settings.xml").doesNotContain("${IDE_HOME}").doesNotContain("${MVN_VERSION}");
    verifyLoggingOfXmlFiles();
  }

  private void verifyLoggingOfXmlFiles() {
    Path workspace = UPGRADE_SETTINGS_PATH.resolve(IdeContext.FOLDER_SETTINGS).resolve("intellij").resolve(IdeContext.FOLDER_WORKSPACE).resolve("TestXml.xml")
        .toAbsolutePath();
    //assert
    assertThat(context).logAtWarning().hasMessage(
        "The XML file " + workspace + " does not contain the XML merge namespace and seems outdated. For details see:\n"
            + "https://github.com/devonfw/IDEasy/blob/main/documentation/configurator.adoc#xml-merger");
  }
}
