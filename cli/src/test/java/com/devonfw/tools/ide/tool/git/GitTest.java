package com.devonfw.tools.ide.tool.git;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelperMock;
import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Git}.
 */
class GitTest extends AbstractIdeContextTest {

  private static final String TOOL_VERSION = "2.45.0";

  private static final String LATEST_WINGET_VERSION = "2.56.0.4";

  /** The version query command the package manager based version detection issues on macOS (see {@link NativePackageManager#BREW}). */
  private static final String BREW_VERSION_QUERY = "brew list --versions git";

  /** The version query command the package manager based version detection issues on Linux (see {@link NativePackageManager#APT}). */
  private static final String APT_VERSION_QUERY = "dpkg-query -W -f=${db:Status-Status}|${Version} git";

  private static final String WINGET_INSTALL_COMMAND = "winget install --exact --source winget Git.Git";

  /** The exit code of winget if the latest version is already installed (APPINSTALLER_CLI_ERROR_UPDATE_NOT_APPLICABLE). */
  private static final int WINGET_NO_APPLICABLE_UPDATE = 0x8A15002B;
  /**
   * A {@link Git} that reports only the given native package managers as available, so a specific OS flow (apt on Linux, brew on macOS, winget on Windows) is
   * exercised.
   *
   * @param context the {@link IdeTestContext}.
   * @param availablePackageManagers the package managers {@code isPackageManagerAvailable} should report as available.
   * @return the {@link Git}.
   */
  private static Git git(IdeTestContext context, NativePackageManager... availablePackageManagers) {

    return new Git(context) {
      @Override
      protected boolean isPackageManagerAvailable(NativePackageManager packageManager) {
        return Arrays.asList(availablePackageManagers).contains(packageManager);
      }
    };
  }

  /**
   * Sets a mocked {@link ProcessContext} (as returned by {@code newProcess()}) whose version query yields the given single-line output, so that the package
   * manager based version detection can be simulated without running the real package manager.
   *
   * @param context the {@link IdeTestContext}.
   * @param command the command whose output should be simulated.
   * @param output the single output line the version query should yield, or {@code null} to simulate that nothing is installed.
   */
  private void processContextWithVersionOutput(IdeTestContext context, String command, String output) {

    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    List<OutputMessage> outputMessages = (output == null) ? List.of() : List.of(new OutputMessage(false, output));
    ProcessResult result = new ProcessResultImpl("bash", command, ProcessResult.SUCCESS, true, outputMessages);
    Mockito.when(processContext.run(ProcessMode.DEFAULT_CAPTURE)).thenReturn(result);
    context.setProcessContext(processContext);
  }

  /**
   * Returns an {@link IdeTestContext} (Windows x64) where neither git nor bash can be found, independent of the PATH of the machine running the test.
   *
   * @return the {@link IdeTestContext}.
   */
  private static IdeTestContext windowsContextWithoutGitInPath() {

    IdeTestContext supportContext = newContext("find-bash-git", "project/workspaces", true);
    Path home = supportContext.getUserHome();
    FileAccess fileAccess = supportContext.getFileAccess();
    Path environmentFile = home.resolve("environment.properties");
    Properties properties = fileAccess.readProperties(environmentFile);
    properties.put("PATH", home.resolve("notexisting").toAbsolutePath().toString());
    properties.remove("BASH_PATH");
    fileAccess.writeProperties(properties, environmentFile);
    IdeTestContext context = new IdeTestContext(home, IdeLogLevel.DEBUG, null);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    return context;
  }

  /**
   * Sets a mocked {@link ProcessContext} that simulates winget.
   *
   * @param context the {@link IdeTestContext}.
   * @param exitCode the exit code of winget.
   * @param versionAfterInstall the version registered by the installer that is run by winget or {@code null} if nothing gets installed.
   * @return the mocked {@link ProcessContext}.
   */
  private static ProcessContext wingetProcess(IdeTestContext context, int exitCode, String versionAfterInstall) {

    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    Mockito.when(processContext.run()).thenAnswer(invocation -> {
      if (versionAfterInstall != null) {
        setGitInRegistry(context, versionAfterInstall);
      }
      return exitCode;
    });
    context.setProcessContext(processContext);
    return processContext;
  }

  private static void setGitInRegistry(IdeTestContext context, String version) {

    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Git", new WindowsAppInstallation(version, null, null, null));
  }

  private static void setGitOnPath(IdeTestContext context) {

    Path binPath = context.getUserHome().resolve("portable-git/bin");
    context.getFileAccess().mkdirs(binPath);
    context.getFileAccess().writeFileContent("", binPath.resolve("git.exe"));
    context.getPath().setPath("git", binPath);
  }

  private static String wingetCommand(String... additionalArgs) {

    return WINGET_INSTALL_COMMAND + " " + String.join(" ", additionalArgs);
  }

  private static ToolInstallRequest installRequest(GenericVersionRange version) {

    ToolInstallRequest request = ToolInstallRequest.ofDirect();
    request.setRequested(new ToolEditionAndVersion(version));
    return request;
  }

  /**
   * Verifies that the installed Git edition and version are resolved from Homebrew ({@code brew list --versions git}) on macOS, mirroring the Linux flow.
   */
  @Test
  void testGetInstalledEditionAndVersionOnMac() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    processContextWithVersionOutput(context, BREW_VERSION_QUERY, "git " + TOOL_VERSION);
    Git git = git(context, NativePackageManager.BREW);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that on macOS the installed Git version is {@code null} when no git formula is installed.
   */
  @Test
  void testGetInstalledEditionAndVersionOnMacWhenNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    processContextWithVersionOutput(context, BREW_VERSION_QUERY, null);
    Git git = git(context, NativePackageManager.BREW);

    // act + assert
    assertThat(git.getInstalledEditionAndVersion()).isNull();
    assertThat(git.getInstalledVersion()).isNull();
  }

  /**
   * Verifies that the installed Git edition and version are resolved from apt ({@code dpkg-query}) on Linux.
   */
  @Test
  void testGetInstalledEditionAndVersionOnLinux() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    processContextWithVersionOutput(context, APT_VERSION_QUERY, "installed|" + TOOL_VERSION);
    Git git = git(context, NativePackageManager.APT);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that on Linux the installed Git version is {@code null} when git is not installed (the apt version query yields no installed version).
   */
  @Test
  void testGetInstalledEditionAndVersionOnLinuxWhenNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    processContextWithVersionOutput(context, APT_VERSION_QUERY, null);
    Git git = git(context, NativePackageManager.APT);

    // act + assert
    assertThat(git.getInstalledEditionAndVersion()).isNull();
    assertThat(git.getInstalledVersion()).isNull();
  }

  /**
   * Verifies that on Windows the installed Git edition and version are resolved from the Windows registry using the app name {@code Git}.
   */
  @Test
  void testGetInstalledEditionAndVersionOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    setGitInRegistry(context, TOOL_VERSION);
    Git git = new Git(context);

    // act
    EditionAndVersion editionAndVersion = git.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("git");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
  }

  /**
   * Verifies that Git can be installed outside of a project (see #2273).
   */
  @Test
  void testIdeHomeNotRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act + assert
    assertThat(new Git(context).isIdeHomeRequired()).isFalse();
  }

  /**
   * Verifies the installation of Git on Windows if neither Git nor bash are available yet: winget is run via cmd instead of bash and shows the installation
   * wizard, no agreement is accepted on behalf of the user, the URL metadata (that can only be cloned with Git) is never accessed and the tool is not run
   * afterwards, as it is only available in a new terminal.
   */
  @Test
  void testInstallOnWindowsViaWinget() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    ToolRepository defaultRepository = Mockito.mock(ToolRepository.class);
    context.setDefaultToolRepository(defaultRepository);
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, LATEST_WINGET_VERSION);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of(LATEST_WINGET_VERSION));
    assertThat(installation.newInstallation()).isTrue();
    assertThat(installation.installedAsynchronously()).isTrue();
    Mockito.verify(processContext).executable("cmd");
    Mockito.verify(processContext).addArgs("/c", wingetCommand("--interactive"));
    Mockito.verifyNoInteractions(defaultRepository);
  }

  /**
   * Verifies that long paths are enabled in the git configuration after Git was installed on Windows. On a fresh machine this is skipped during the
   * installation of IDEasy as Git is not installed yet (see #1293).
   */
  @Test
  void testInstallOnWindowsEnablesLongpaths() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    wingetProcess(context, ProcessResult.SUCCESS, LATEST_WINGET_VERSION);
    Git git = git(context, NativePackageManager.WINGET);
    Path gitconfig = context.getUserHome().resolve(".gitconfig");

    // act
    git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(gitconfig).hasContent("[core]\nlongpaths = true");
  }

  /**
   * Verifies that the first installation is silent in batch mode, since nobody can operate the installation wizard or answer the agreement of the source. The
   * agreements of the package are still not accepted on behalf of the user.
   */
  @Test
  void testInstallOnWindowsInBatchModeIsSilent() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    context.getStartContext().setBatchMode(true);
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, LATEST_WINGET_VERSION);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    Mockito.verify(processContext).addArgs("/c", wingetCommand("--silent", "--accept-source-agreements", "--disable-interactivity"));
  }

  /**
   * Verifies that a version requested explicitly is passed to winget and an installed version that differs from it gets replaced.
   */
  @Test
  void testInstallOnWindowsPinsRequestedVersion() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitInRegistry(context, LATEST_WINGET_VERSION);
    setGitOnPath(context);
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, TOOL_VERSION);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.of(TOOL_VERSION)));

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
    Mockito.verify(processContext).addArgs("/c", wingetCommand("--version", TOOL_VERSION, "--silent"));
  }

  /**
   * Verifies that a missing winget results in an error telling the user how to install Git manually.
   */
  @Test
  void testInstallOnWindowsFailsWithHintIfWingetIsNotAvailable() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, null);
    Git git = git(context);
    ToolInstallRequest request = installRequest(VersionIdentifier.LATEST);

    // act + assert
    assertThatThrownBy(() -> git.install(request)).isInstanceOf(CliException.class).hasMessageContaining("winget")
        .hasMessageContaining(IdeContext.WINDOWS_GIT_DOWNLOAD_URL);
    Mockito.verify(processContext, Mockito.never()).run();
  }

  /**
   * Verifies that winget is run like any other package manager also if Git is already installed, so that Git gets upgraded if a newer version is available.
   */
  @Test
  void testInstallOnWindowsUpgradesViaWingetIfAlreadyInstalled() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitInRegistry(context, TOOL_VERSION);
    setGitOnPath(context);
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, LATEST_WINGET_VERSION);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of(LATEST_WINGET_VERSION));
    assertThat(installation.installedAsynchronously()).isFalse();
    Mockito.verify(processContext).addArgs("/c", wingetCommand("--silent"));
  }

  /**
   * Verifies that winget is not run if Git was found on the PATH without being known to the registry (e.g. PortableGit or Scoop), as winget cannot manage
   * such a Git and would install a second one with the installation wizard. Also a missing or blocked winget must not break such a Git.
   */
  @Test
  void testInstallOnWindowsSkipsWingetIfOnlyFoundOnPath() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitOnPath(context);
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, null);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(installation.newInstallation()).isFalse();
    assertThat(installation.installedAsynchronously()).isFalse();
    Mockito.verify(processContext, Mockito.never()).run();
  }

  /**
   * Verifies that an installed Git that is not on the PATH (e.g. installed with the option "Use Git from Git Bash only") results in an error instead of
   * silently skipping the git command.
   */
  @Test
  void testInstallOnWindowsFailsIfInstalledGitIsNotOnPath() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitInRegistry(context, TOOL_VERSION);
    wingetProcess(context, WINGET_NO_APPLICABLE_UPDATE, null);
    Git git = git(context, NativePackageManager.WINGET);
    ToolInstallRequest request = installRequest(VersionIdentifier.LATEST);

    // act + assert
    assertThatThrownBy(() -> git.install(request)).isInstanceOf(CliException.class).hasMessageContaining("PATH");
  }

  /**
   * Verifies that a failing upgrade only logs a warning if Git is already installed, so that the installed Git can still be used (e.g. offline).
   */
  @Test
  void testInstallOnWindowsKeepsInstalledVersionIfWingetFails() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitInRegistry(context, TOOL_VERSION);
    setGitOnPath(context);
    wingetProcess(context, 1, null);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of(TOOL_VERSION));
    assertThat(installation.newInstallation()).isFalse();
    assertThat(context).logAtWarning().hasMessageContaining("Failed to install Git");
  }

  /**
   * Verifies that a version that could inject further commands into the command line of {@code cmd} is rejected before winget is run.
   */
  @Test
  void testInstallOnWindowsRejectsUnsafeVersion() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    ProcessContext processContext = wingetProcess(context, ProcessResult.SUCCESS, null);
    Git git = git(context, NativePackageManager.WINGET);
    ToolInstallRequest request = installRequest(VersionIdentifier.of("2.45.0&calc"));

    // act + assert
    assertThatThrownBy(() -> git.install(request)).isInstanceOf(CliException.class).hasMessageContaining("2.45.0&calc");
    Mockito.verify(processContext, Mockito.never()).run();
  }

  /**
   * Verifies that it is no error if the latest version is already installed.
   */
  @Test
  void testInstallOnWindowsAcceptsThatNoUpdateIsApplicable() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    setGitInRegistry(context, LATEST_WINGET_VERSION);
    setGitOnPath(context);
    ProcessContext processContext = wingetProcess(context, WINGET_NO_APPLICABLE_UPDATE, null);
    Git git = git(context, NativePackageManager.WINGET);

    // act
    ToolInstallation installation = git.install(installRequest(VersionIdentifier.LATEST));

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of(LATEST_WINGET_VERSION));
    Mockito.verify(processContext).addArgs("/c", wingetCommand("--silent"));
  }
}
