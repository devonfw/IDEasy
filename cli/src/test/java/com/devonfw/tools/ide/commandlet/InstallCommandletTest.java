package com.devonfw.tools.ide.commandlet;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.tool.repository.DefaultToolRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link InstallCommandlet}.
 */
@WireMockTest
class InstallCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_INSTALL = "install";

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is null.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testInstallCommandletRunWithVersion(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
  }

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is set.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testInstallCommandletRunWithVersionAndVersionIdentifier(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String version17 = "17.0.6";
    install.version.setValueAsString(version17, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtSuccess().hasMessage("Successfully installed java in version " + version17);
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version17);

    // now we install a different version

    // arrange
    String version21 = "21.0.8_9";
    install.version.setValueAsString(version21, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtSuccess()
        .hasMessage("Successfully installed java in version " + version21 + " replacing previous version " + version17 + " of java");
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version21);

    // now we install the initial version again that should just change the symlink

    // arrange
    install.version.setValueAsString(version17, context);
    // act
    install.run();
    // assert
    assertTestInstall(context);
    assertThat(context).logAtDebug().hasMessage("Version " + version17 + " of tool java is already installed at " + context.getSoftwareRepositoryPath().resolve(
        DefaultToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve(version17));
    assertThat(context).logAtSuccess()
        .hasMessage("Successfully installed java in version " + version17 + " replacing previous version " + version21 + " of java");
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version17);
  }

  private void assertTestInstall(IdeContext context) {

    assertThat(context.getSoftwarePath().resolve("java")).exists();
    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java.cmd")).exists();
    } else if (context.getSystemInfo().isLinux() || context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    }
  }

  /**
   * Test of {@link InstallCommandlet} with --skip-updates flag. Verifies that when a matching version is already installed, no download or update is performed
   * even when a more recent version is available.
   * <p>
   * Test scenario: Install java 17.0.6, then try to install with pattern "17*" (which would match 17.0.10 available in test data). Without --skip-updates:
   * would download and install 17.0.10 (the latest 17.x version) With --skip-updates: should skip update and keep 17.0.6 since it matches the pattern
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testInstallCommandletWithSkipUpdates(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - create context and install initial version 17.0.6
    // Note: Test data has 17.0.6 and 17.0.10 available, so "17*" pattern would resolve to 17.0.10
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String installedVersion = "17.0.6";
    install.version.setValueAsString(installedVersion, context);
    install.run();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(installedVersion);

    // arrange - enable --skip-updates mode and configure wildcard version pattern
    // Pattern "17*" matches installed 17.0.6 but would normally resolve to latest 17.0.10
    context.getStartContext().setSkipUpdatesMode(true);
    install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("17*", context);

    // Record the log entry count before the action
    int logCountBefore = context.getLogger().getEntries().size();

    // act - try to install with --skip-updates
    // Should skip the update to 17.0.10 since 17.0.6 matches the pattern
    install.run();

    // assert - should NOT download 17.0.10, should stay on 17.0.6
    List<IdeLogEntry> newLogEntries = context.getLogger().getEntries().subList(logCountBefore, context.getLogger().getEntries().size());
    boolean hasDownloadMessage = newLogEntries.stream().anyMatch(e -> e.message().contains("Trying to download"));
    assertThat(hasDownloadMessage).as("Should not download when --skip-updates is enabled and version matches pattern").isFalse();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(installedVersion);
  }

  /**
   * Test of {@link InstallCommandlet} with --skip-updates flag when tool is not yet installed. Verifies that even with --skip-updates enabled, a tool is still
   * installed if it doesn't exist yet.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testInstallCommandletWithSkipUpdatesInstallsWhenNotInstalled(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - create context with --skip-updates enabled but tool NOT installed
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    context.getStartContext().setSkipUpdatesMode(true);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("17*", context);

    // Verify tool is not installed yet
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).doesNotExist();

    // act - install with --skip-updates when tool is not installed
    install.run();

    // assert - SHOULD download and install because tool was not installed (regardless of --skip-updates)
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists();
    assertThat(context).log().hasMessageContaining("Trying to download");
  }

  /**
   * Test of {@link InstallCommandlet} with --skip-updates flag when installed version does NOT match configured version. Verifies that even with --skip-updates
   * enabled, an update occurs if the installed version doesn't match the pattern.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testInstallCommandletWithSkipUpdatesInstallsWhenVersionMismatch(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - install version 17.0.6
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("17.0.6", context);
    install.run();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("17.0.6");

    // arrange - enable --skip-updates and configure version that does NOT match installed version
    // Changing from "17*" to "21*" means installed 17.0.6 does NOT match
    context.getStartContext().setSkipUpdatesMode(true);
    install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("21*", context);

    int logCountBefore = context.getLogger().getEntries().size();

    // act - install with --skip-updates but with non-matching version
    install.run();

    // assert - SHOULD download and install 21.x because installed 17.0.6 does NOT match pattern "21*"
    List<IdeLogEntry> newLogEntries = context.getLogger().getEntries().subList(logCountBefore, context.getLogger().getEntries().size());
    boolean hasDownloadMessage = newLogEntries.stream().anyMatch(e -> e.message().contains("Trying to download") && e.message().contains("21."));
    assertThat(hasDownloadMessage).as("Should download when --skip-updates is enabled but version does not match").isTrue();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("21.0.8_9");
  }
}
