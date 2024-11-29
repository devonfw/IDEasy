package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link UpgradeSettingsCommandlet} .
 */
public class UpgradeSettingsCommandletTest extends AbstractIdeContextTest {

  private static final Path UPGRADESETTINGS_PATH = Path.of("target/test-projects/upgrade-settings/project/");

  private static final String PROJECT_UPGRADESETTINGS = "upgrade-settings";
  private final IdeTestContext context = newContext(PROJECT_UPGRADESETTINGS);

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
    assertThat(Files.exists(UPGRADESETTINGS_PATH.resolve("conf/ide.properties"))).isTrue();
    assertThat(Files.exists(UPGRADESETTINGS_PATH.resolve("settings/ide.properties"))).isTrue();
    assertThat(Files.exists(UPGRADESETTINGS_PATH.resolve("workspaces/main/ide.properties"))).isTrue();
    //assert that file content was changed
    assertThat(UPGRADESETTINGS_PATH.resolve("conf/ide.properties")).content()
        .isEqualTo("#********************************************************************************\n"
            + "# This file contains project specific environment variables defined by the user\n"
            + "#********************************************************************************\n"
            + "\n"
            + "MVN_VERSION=test\n"
            + "\n"
            + "HOME_DIR=test\n"
            + "IDE_HOME=test\n"
            + "JAVA_VERSION=test\n"
            + "IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED=false");
  }

  /**
   * Ensure that the custom-tools.json was created with the correct content.
   */
  @Test
  public void testCustomJsonFileCreation() {
    // arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    // act
    upgradeSettingsCommandlet.run();
    // assert that custom-tools exists
    assertThat(Files.exists(UPGRADESETTINGS_PATH.resolve("settings/custom-tools.json"))).isTrue();
    //assert that custom-tools has the correct content
    assertThat(UPGRADESETTINGS_PATH.resolve("settings/custom-tools.json")).content().isEqualTo(
        "{\"tools\":[{\"os-agnostic\":true,\"arch-agnostic\":true,\"name\":\"jboss-eap\",\"version\":\"7.1.4.GA\"},{\"os-agnostic\":false,\"arch-agnostic\":false,\"name\":\"firefox\",\"version\":\"70.0.1\"}],\"url\":\"https:\\/\\/host.tld\\/projects\\/my-project\"}");
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
    assertThat(Files.isDirectory(UPGRADESETTINGS_PATH.resolve("settings/templates"))).isTrue();
    assertThat(Files.isDirectory(UPGRADESETTINGS_PATH.resolve("settings/repositories"))).isTrue();
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
    assertThat(UPGRADESETTINGS_PATH.resolve("settings/workspace/testVariableSyntax")).content().isEqualTo("$[IDE_HOME]\n"
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
    // act
    upgradeSettingsCommandlet.run();
    //assert
    assertThat(context).logAtWarning().hasMessageContaining(
        "The XML file C:\\projects\\IDEasy\\workspaces\\main\\IDEasy\\cli\\target\\test-projects\\upgrade-settings\\project\\settings\\intellij\\workspace\\TestXml.xml does not contain the required 'xmlns:merge' attribute.");
    assertThat(context).logAtWarning()
        .hasMessageContaining("For further information, please visit https://github.com/devonfw/IDEasy/blob/main/documentation/configurator.adoc#xml-merger");
  }
}
