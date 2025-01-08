package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link UpgradeSettingsCommandlet} .
 */
public class UpgradeSettingsCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_UPGRADE_SETTINGS = "upgrade-settings";
  private final IdeTestContext context = newContext(PROJECT_UPGRADE_SETTINGS);
  private static final Path UPGRADE_SETTINGS_PATH = TEST_PROJECTS_COPY.resolve(PROJECT_UPGRADE_SETTINGS).resolve("project");

  /**
   * Ensure that all devon.properties are renamed to ide.properties and that the variables inside have been adjusted.
   */
  @Test
  public void testDevonPropertiesUpgrade() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    // assert that files where renamed
    // FIXME assertThat(UPGRADE_SETTINGS_PATH.resolve("home/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("conf/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("workspaces/main/ide.properties")).exists();
    //assert that file content was changed
    assertThat(UPGRADE_SETTINGS_PATH.resolve("conf/ide.properties")).content()
        .isEqualTo("#********************************************************************************\n"
            + "# This file contains project specific environment variables defined by the user\n"
            + "#********************************************************************************\n"
            + "\n"
            + "MVN_VERSION=test\n");
  }

  /**
   * Ensure that the ide-custom-tools.json was created with the correct content.
   */
  @Test
  public void testCustomJsonFileCreation() throws Exception {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    // assert
    Path customToolsJsonFile = UPGRADE_SETTINGS_PATH.resolve("settings").resolve(IdeContext.FILE_CUSTOM_TOOLS);
    assertThat(customToolsJsonFile).exists();
    assertThat(Files.readString(customToolsJsonFile).replace("\r", "").replace("\n", "").replace(" ", "")).isEqualTo(
        "{\"url\":\"https://host.tld/projects/my-project\",\"tools\":[{\"name\":\"jboss-eap\",\"version\":\"7.1.4.GA\",\"os-agnostic\":true,\"arch-agnostic\":true},{\"name\":\"firefox\",\"version\":\"70.0.1\",\"os-agnostic\":false,\"arch-agnostic\":false}]}");
  }

  /**
   * Ensure that settings/devon and settings/projects are renamed.
   */
  @Test
  public void testIfFolderAreRenamed() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    //assert
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/repositories/IDEasy.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/templates/conf/ide.properties")).exists();
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/templates/conf/mvn/settings.xml")).exists();
  }

  /**
   * Ensure that the variable syntax is changed from CURLY into ANGLED.
   */
  @Test
  public void testIfVariableSyntaxIsChanged() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    //assert
    assertThat(UPGRADE_SETTINGS_PATH.resolve("settings/workspace/testVariableSyntax.txt")).content().isEqualTo("$[IDE_HOME]\n"
        + "This is a test text,this is a test text,this is a test text,this is a test text,\n"
        + "this is a test text,\n"
        + "this is a test text,$[MVN_VERSION]this is a test text,this is a test text,$[IDE_HOME]/settings\n"
        + "this is a test text,this is a test text,this is a test text,this is a test text,\n");
  }

  /**
   * Ensure that xml files that need to be adjusted are logged and the link to documentation is logged.
   */
  @Test
  public void testLoggingOfXmlFiles() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    Path workspace = UPGRADE_SETTINGS_PATH.resolve(IdeContext.FOLDER_SETTINGS).resolve("intellij").resolve(IdeContext.FOLDER_WORKSPACE).resolve("TestXml.xml")
        .toAbsolutePath();
    // act
    upgradeSettingsCommandlet.run();
    //assert
    assertThat(context).logAtWarning().hasMessage(
        "The XML file " + workspace + " does not contain the XML merge namespace and seems outdated. For details see:\n"
            + "https://github.com/devonfw/IDEasy/blob/main/documentation/configurator.adoc#xml-merger");
  }
}
