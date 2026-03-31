package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Test of {@link VersionGetCommandlet}.
 */
class VersionGetCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_SETTINGS = "settings";
  private static final String PROJECT = "edition-version-get-uninstall";

  /**
   * Test of {@link VersionGetCommandlet} run, when Installed Version is null but configured version is set.
   */
  @Test
  void testVersionGetCommandletNotInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("az", context);
    // act
    versionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("No installation of tool az was found."),
        IdeLogEntry.ofProcessable("The configured version for tool az is 1.0.1"),
        IdeLogEntry.ofProcessable("ide install az"));
  }

  /**
   * Test of {@link VersionGetCommandlet} run, when --installed flag is set, --configured is not set and installed version is null.
   */
  @Test
  void testVersionGetCommandletNotInstalledWithInstalledFlagRunThrowsException() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("tomcat", context);
    versionGet.installed.setValue(true);
    // act/assert
    assertThrows(CliException.class, () -> versionGet.run());
  }

  /**
   * Test of {@link VersionGetCommandlet} run, when --configured flag is set, configured version is not set and installed version is null.
   */
  @Test
  void testVersionGetCommandletNotInstalledConfiguredWithConfiguredFlagRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("az", context);
    versionGet.configured.setValue(true);
    // act
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("*");
  }

  /**
   * Test of {@link VersionGetCommandlet} run, when --configured flag is set and Installed Version is null.
   */
  @Test
  void testVersionGetCommandletNotInstalledWithConfigured() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    versionGet.tool.setValueAsString("az", context);
    versionGet.configured.setValue(true);
    // act
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("1.0.1");
  }

  /**
   * Test of {@link VersionGetCommandlet} run, where "configured" flag is set and the configured version is 3.9.1.
   */
  @Test
  void testVersionGetCommandletConfiguredRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("3.9.1");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the "installed" flag, where the installed version is 3.9.4.
   */
  @Test
  void testVersionGetCommandletInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
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
  void testVersionGetCommandletConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("*");
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the "configured" and the "installed" flag, where the configured version is "any" (*).
   */
  @Test
  void testVersionGetCommandletConfiguredInstalledRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.installed.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("The installed version for tool mvn is 3.9.4"),
        IdeLogEntry.ofProcessable("The configured version for tool mvn is *"),
        IdeLogEntry.ofProcessable("ide install mvn"));
  }

  /**
   * Test of {@link VersionGetCommandlet} run with the "configured" and the "installed" flag, where the configured version is 3.9.1.
   */
  @Test
  void testVersionGetCommandletConfiguredInstalledSpecificRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.configured.setValue(true);
    versionGet.installed.setValue(true);
    versionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("The installed version for tool mvn is 3.9.4"),
        IdeLogEntry.ofProcessable("The configured version for tool mvn is 3.9.1"),
        IdeLogEntry.ofProcessable("ide install mvn"));
  }

  /**
   * Test of {@link VersionGetCommandlet} run, where a specific version is installed (mvn 3.9.4) but no specific version is configured (configured version *).
   */
  @Test
  void testVersionGetCommandletMatchInstalledToConfiguredStarRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_SETTINGS, null, false);
    VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
    // act
    versionGet.tool.setValueAsString("mvn", context);
    versionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("The installed version for tool mvn is 3.9.4"),
        IdeLogEntry.ofProcessable("The configured version for tool mvn is *"),
        IdeLogEntry.ofProcessable("ide install mvn")
    );
  }

}
