package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

/**
 * Integration test of {@link VersionGetCommandlet}.
 */
public class VersionGetCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link VersionGetCommandlet} run, when Installed Version is null.
   */
  @Test
  public void testVersionGetCommandletRunThrowsCliException() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("java", context);
    // act
    versionGet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "No installation of tool java was found.");
    assertLogMessage(context, IdeLogLevel.INFO, "The configured version for tool java is 17*");
    assertLogMessage(context, IdeLogLevel.INFO, "To install that version call the following command:");
    assertLogMessage(context, IdeLogLevel.INFO, "ide install java");
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
    assertLogMessage(context, IdeLogLevel.INFO, "3.9.4");
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
    assertLogMessage(context, IdeLogLevel.INFO, "3.9.*");
  }
}
