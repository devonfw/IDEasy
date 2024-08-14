package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link VersionGetCommandlet}.
 */
public class VersionGetCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_SETTINGS = "settings";

  /**
   * Test of {@link VersionGetCommandlet} run, when Installed Version is null.
   */
  @Test
  public void testVersionGetCommandletNotInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("java", context);
    // act
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasEntries("No installation of tool java was found.", "The configured version for tool java is 17*",
        "To install that version call the following command:", "ide install java");
  }

  @Test
  public void testVersionGetCommandletNotInstalledRunInstalledFlag() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("java", context);
    versionGet.installed.setValue(true);
    // act
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("No installation of tool java was found.");
    assertThat(context).logAtInfo().hasMessage("The configured version for tool java is 17*");
    assertThat(context).logAtInfo().hasMessage("To install that version call the following command:");
    assertThat(context).logAtInfo().hasMessage("ide install java");
  }

  /**
   * Test of {@link VersionGetCommandlet} run.
   */
  @Test
  public void testVersionGetCommandletConfiguredRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("3.9.1");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the installed flag, where the installed version is 3.9.4.
   */
  @Test
  public void testVersionGetCommandletInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.installed.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("3.9.4");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the "configured" flag, where the configured version is "any" (*).
   */
  @Test
  public void testVersionGetCommandletConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("*");
  }

  /**
   * Test of {@link VersionGetCommandlet} run, where a specific version is installed (mvn 3.9.4) but no specific version is configured (configured version *)
   * The expected output is to be 3.9.4 only.
   */
  @Test
  public void testVersionGetCommandletMatchInstalledToConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("3.9.4");
  }

  /**
   * Test of {@link VersionGetCommandlet} run, where the tool is neither installed nor configured.
   */
  @Test
  public void testVersionGetCommandletNeitherInstalledNorConfigured() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("java", context);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("No installation of tool java was found.");
    assertThat(context).logAtInfo().hasMessage("The configured version for tool java is *");
    assertThat(context).logAtInfo().hasMessage("To install that version call the following command:");
    assertThat(context).logAtInfo().hasMessage("ide install java");
  }

}
