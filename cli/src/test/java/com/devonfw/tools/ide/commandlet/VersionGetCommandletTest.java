package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link VersionGetCommandlet}.
 */
public class VersionGetCommandletTest extends AbstractIdeContextTest {

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
  public void testVersionGetCommandletInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.installed.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("3.9.4");
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
}
