package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.tool.repository.DefaultToolRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link InstallCommandlet}.
 */
@WireMockTest
class InstallCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_INSTALL = "install";

  /**
   * Verifies that installing Git does not require {@code IDE_HOME} (i.e. a project), so that it can be installed on a fresh machine that has IDEasy but no
   * project yet (see #2273), whereas every other tool (local tools like java and other global tools like docker) still requires a project.
   */
  @Test
  void testIdeHomeOnlyRequiredForNonGitTools() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INSTALL);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);

    // act & assert
    assertThat(install.isIdeHomeRequired()).as("no tool selected").isFalse();

    install.tool.setValueAsString("git", context);
    assertThat(install.isIdeHomeRequired()).as("git (bootstrap, #2273)").isFalse();

    install.tool.setValueAsString("java", context);
    assertThat(install.isIdeHomeRequired()).as("local tool 'java'").isTrue();

    // other global tools (e.g. docker) still require a project: only git is exempted (it can be installed to bootstrap itself)
    install.tool.setValueAsString("docker", context);
    assertThat(install.isIdeHomeRequired()).as("global tool 'docker'").isTrue();
  }

  /**
   * Verifies that installing Git requires {@code IDE_ROOT} like any other tool (the URL metadata is stored below it, see #2273) - the only requirement Git
   * does not have is {@code IDE_HOME} (see {@link #testIdeHomeOnlyRequiredForNonGitTools()}).
   */
  @Test
  void testIdeRootRequiredForGitLikeAnyOtherTool() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INSTALL);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);

    // act & assert
    assertThat(install.isIdeRootRequired()).as("no tool selected").isFalse();

    install.tool.setValueAsString("git", context);
    assertThat(install.isIdeRootRequired()).as("git requires IDE_ROOT to store the URL metadata").isTrue();

    install.tool.setValueAsString("java", context);
    assertThat(install.isIdeRootRequired()).as("local tool 'java'").isTrue();
  }

  /**
   * Verifies that {@code ide install git «version»} works outside of a project, where no settings exist to persist the version to (see #2273).
   *
   * @param directory the temporary directory that is not part of an IDEasy project.
   */
  @Test
  void testInstallGitWithVersionOutsideOfProject(@TempDir Path directory) {

    // arrange
    IdeTestContext context = new IdeTestContext(directory, IdeLogLevel.DEBUG, null);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    assertThat(context.getIdeHome()).isNull();
    GitContext gitContext = Mockito.mock(GitContext.class);
    Mockito.when(gitContext.findGit()).thenReturn(Path.of("C:/Program Files/Git/bin/git"));
    context.setGitContext(gitContext);
    context.setUrlsPath(directory);
    context.getFileAccess().mkdirs(directory.resolve("git"));
    VersionIdentifier version = VersionIdentifier.of("2.56.0.4");
    ToolRepository toolRepository = Mockito.mock(ToolRepository.class);
    Mockito.when(toolRepository.resolveVersion(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(version);
    Mockito.when(toolRepository.findSecurity("git", "git")).thenReturn(ToolSecurity.getEmpty());
    Mockito.when(toolRepository.download(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(directory.resolve("git-installer.exe"));
    context.setDefaultToolRepository(toolRepository);
    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    Mockito.when(processContext.run(ProcessMode.DEFAULT))
        .thenReturn(new ProcessResultImpl("installer", "installer", ProcessResult.SUCCESS, true, List.of()));
    context.setProcessContext(processContext);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("git", context);
    install.version.setValueAsString(version.toString(), context);

    // act + assert
    assertThatCode(install::run).doesNotThrowAnyException();
  }

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
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed java in version " + version17);
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
        .hasMessageContaining("Successfully installed java in version " + version21 + " replacing previous version " + version17 + " of java");
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
        .hasMessageContaining("Successfully installed java in version " + version17 + " replacing previous version " + version21 + " of java");
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
    int logCountBefore = context.getTestStartContext().getEntries().size();

    // act - try to install with --skip-updates
    // Should skip the update to 17.0.10 since 17.0.6 matches the pattern
    install.run();

    // assert - should NOT download 17.0.10, should stay on 17.0.6
    List<IdeLogEntry> newLogEntries = context.getTestStartContext().getEntries().subList(logCountBefore, context.getTestStartContext().getEntries().size());
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

    int logCountBefore = context.getTestStartContext().getEntries().size();

    // act - install with --skip-updates but with non-matching version
    install.run();

    // assert - SHOULD download and install 21.x because installed 17.0.6 does NOT match pattern "21*"
    List<IdeLogEntry> newLogEntries = context.getTestStartContext().getEntries().subList(logCountBefore, context.getTestStartContext().getEntries().size());
    boolean hasDownloadMessage = newLogEntries.stream().anyMatch(e -> e.message().contains("Trying to download") && e.message().contains("21."));
    assertThat(hasDownloadMessage).as("Should download when --skip-updates is enabled but version does not match").isTrue();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("21.0.8_9");
  }

  /**
   * Test of {@link InstallCommandlet} when offline but tool version is already downloaded. Verifies that the tool can be installed from cached download even
   * when offline.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testInstallCommandletOfflineWithCachedDownload(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - install java while online to ensure it's downloaded
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String version = "17.0.6";
    install.version.setValueAsString(version, context);
    install.run();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version);

    // arrange - delete the installed tool but keep the cached download
    context.getFileAccess().delete(context.getSoftwarePath().resolve("java"));
    context.getFileAccess().delete(context.getSoftwareRepositoryPath().resolve(DefaultToolRepository.ID_DEFAULT).resolve("java"));

    // arrange - simulate offline mode
    context.getNetworkStatus().simulateNetworkError();

    // act - try to install again while offline
    install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString(version, context);
    install.run();

    // assert - should successfully install from cached download
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(version);
    // Determine the correct file extension and OS name based on the current OS
    String fileExtension = context.getSystemInfo().isWindows() ? "zip" : "tgz";
    String osName = context.getSystemInfo().getOs().toString().toLowerCase();
    assertThat(context).logAtDebug().hasMessage("Using cached download of java in version " + version + " from "
        + context.getDownloadPath().resolve("default").resolve("java-" + version + "-" + osName + "-x64." + fileExtension) + " (offline mode)");
  }

  /**
   * Test of {@link InstallCommandlet} when offline and tool version is not cached. Verifies that installation fails with appropriate exception when tool is not
   * available locally and we are offline.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testInstallCommandletOfflineWithoutCachedDownload(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - create context without pre-installing anything
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    context.getNetworkStatus().simulateNetworkError();
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String version = "17.0.6";
    install.version.setValueAsString(version, context);

    // act & assert - should fail with CliOfflineException
    CliOfflineException e = assertThrows(CliOfflineException.class, install::run);
    assertThat(e).hasMessageContaining("Not able to download tool java");
    assertThat(e).hasMessageContaining("because we are offline");
  }

  /**
   * Test of {@link InstallCommandlet} when offline and trying to update to a version that is not cached. Verifies that IDEasy continues using the currently
   * installed version instead of blocking the user.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testInstallCommandletOfflineUpdateWithoutCachedDownload(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange - install java 17.0.6 while online
    IdeTestContext context = newContext(PROJECT_INSTALL, wmRuntimeInfo);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String installedVersion = "17.0.6";
    install.version.setValueAsString(installedVersion, context);
    install.run();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(installedVersion);

    // arrange - try to update to 17.0.10 while offline (version not cached)
    context.getNetworkStatus().simulateNetworkError();
    install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    String targetVersion = "17.0.10";
    install.version.setValueAsString(targetVersion, context);

    // act - try to install/update while offline
    install.run();

    // assert - should continue using the old version and log a warning
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(installedVersion);
    assertThat(context).logAtWarning().hasMessage("Cannot download java in version " + targetVersion
        + " because we are offline. Continuing with already installed version " + installedVersion + ".");
  }
}
