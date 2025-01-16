package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Integration test of {@link VersionGetCommandlet}.
 */
public class VersionGetCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_SETTINGS = "settings";
  private static final String PROJECT = "edition-version-get-uninstall";

  /**
   * Test of {@link VersionGetCommandlet} run, when Installed Version is null.
   */
  @Test
  public void testVersionGetCommandletNotInstalledRunThrowsException() {

    // arrange
    IdeTestContext context = newContext(PROJECT);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("java", context);
    // act/assert
    assertThrows(CliException.class, () -> versionGet.run());
  }

  /**
   * Test of {@link VersionGetCommandlet} run.
   */
  @Test
  public void testVersionGetCommandletConfiguredRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("3.9.1");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the installed flag, where the installed version is 3.9.4.
   */
  @Test
  public void testVersionGetCommandletInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.installed.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("3.9.4");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the "configured" flag, where the configured version is "any" (*).
   */
  @Test
  public void testVersionGetCommandletConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("*");
  }

  /**
   * Test of {@link VersionGetCommandlet} run, where a specific version is installed (mvn 3.9.4) but no specific version is configured (configured version *).
   */
  @Test
  public void testVersionGetCommandletMatchInstalledToConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("The installed version for tool mvn is 3.9.4."),
        IdeLogEntry.ofProcessable("The configured version for tool mvn is *."),
        IdeLogEntry.ofProcessable("ide install mvn")
    );
  }

}
