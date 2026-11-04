package com.devonfw.tools.ide.tool.git;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextImpl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfo;
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
import com.devonfw.tools.ide.tool.custom.CustomToolMetadata;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Git}.
 */
@WireMockTest
class GitTest extends AbstractIdeContextTest {

  private static final String TOOL_VERSION = "2.45.0";

  private static final String LATEST_RELEASE_PATH = "/releases/latest";

  private static final String TAG = "v2.56.0.windows.4";

  /** The version query command the package manager based version detection issues on macOS (see {@link NativePackageManager#BREW}). */
  private static final String BREW_VERSION_QUERY = "brew list --versions git";

  /** The version query command the package manager based version detection issues on Linux (see {@link NativePackageManager#APT}). */
  private static final String APT_VERSION_QUERY = "dpkg-query -W -f=${db:Status-Status}|${Version} git";

  /**
   * A {@link Git} that reports only the given native package managers as available, so a specific OS flow (apt on Linux, brew on macOS) is exercised.
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
   * Returns a fresh, mutable {@link IdeTestContext} (Windows x64, no git, no local URL metadata) suitable for testing the latest installer.
   *
   * @return the {@link IdeTestContext}.
   */
  private static IdeTestContext freshWindowsContext() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    context.getFileAccess().delete(context.getUrlsPath()); // simulate the fresh machine without local URL metadata
    return context;
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
   * Verifies that Git declares a Homebrew formula as one of its native packages, so that on macOS it can be installed via {@code brew} the same way it is
   * installed via {@code apt}/{@code zypper} on Linux.
   */
  @Test
  void testGetNativePackagesContainsBrew() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Git git = new Git(context);

    // act + assert
    assertThat(git.getNativePackages().stream().map(pkg -> pkg.getPackageManager())).contains(NativePackageManager.BREW);
  }

  /**
   * Verifies that the Windows registry app name of Git is {@code "Git"} (which differs from the lower case tool name).
   */
  @Test
  void testGetWindowsRegistryAppName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Git git = new Git(context);

    // act + assert
    assertThat(git.getWindowsRegistryAppName()).isEqualTo("Git");
    assertThat(git.getWindowsRegistryAppNames()).containsEntry("git", "Git");
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
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Git", new WindowsAppInstallation(TOOL_VERSION, null, null, null));
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
   * Verifies that the very first installation of Git on Windows (Git is not installed yet) runs the installer interactively so the user can follow the
   * installation wizard.
   */
  @Test
  void testGetInstallerArgumentsInteractiveOnFirstInstallation() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Git git = new Git(context);

    // act
    List<String> gitArguments = git.getInstallerArguments();

    // assert
    assertThat(gitArguments).containsExactly("/NORESTART", "/NOCANCEL", "/SP-");
  }

  /**
   * Verifies that a Git upgrade on Windows (Git already installed) runs the installer fully silent via {@code /VERYSILENT}.
   */
  @Test
  void testGetInstallerArgumentsSilentOnUpgrade() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    helper.setAppInstallationFromRegistry("Git", new WindowsAppInstallation(TOOL_VERSION, null, null, null));
    Git git = new Git(context);

    // act
    List<String> gitArguments = git.getInstallerArguments();

    // assert
    assertThat(gitArguments).containsExactly("/VERYSILENT", "/NORESTART", "/NOCANCEL", "/SP-");
  }

  /**
   * Verifies that the first installation is silent in batch mode, since nobody can operate the installation wizard.
   */
  @Test
  void testGetInstallerArgumentsSilentInBatchMode() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    context.getStartContext().setBatchMode(true);
    Git git = new Git(context);

    // act
    List<String> gitArguments = git.getInstallerArguments();

    // assert
    assertThat(gitArguments).containsExactly("/VERYSILENT", "/NORESTART", "/NOCANCEL", "/SP-");
  }

  /**
   * Verifies that on a fresh Windows machine where neither Git nor the URL metadata are available, the latest installer is used.
   */
  @Test
  void testGetToolRepositoryOnFreshWindowsMachine() {

    // arrange
    IdeTestContext context = freshWindowsContext();
    ToolRepository latestInstallerRepository = Mockito.mock(ToolRepository.class);

    // act + assert
    assertThat(gitWithLatestInstallerRepository(context, latestInstallerRepository).getToolRepository()).isSameAs(latestInstallerRepository);
  }

  /**
   * Verifies that the regular {@link ToolRepository} is used if Git is installed, since the URL metadata can be cloned then and provides checksums.
   */
  @Test
  void testGetToolRepositoryWithGitButWithoutUrlMetadata() {

    // arrange
    IdeTestContext context = freshWindowsContext();
    GitContext gitContext = Mockito.mock(GitContext.class);
    Mockito.when(gitContext.findGit()).thenReturn(Path.of("C:/Program Files/Git/bin/git.exe"));
    context.setGitContext(gitContext);

    // act + assert
    assertThat(new Git(context).getToolRepository()).isSameAs(context.getDefaultToolRepository());
  }

  /**
   * Verifies that the regular {@link ToolRepository} is used without looking for Git when the URL metadata for Git already exists.
   */
  @Test
  void testGetToolRepositoryWhenUrlMetadataExists() {

    // arrange
    IdeTestContext context = freshWindowsContext();
    context.getFileAccess().mkdirs(context.getUrlsPath().resolve("git"));
    GitContext gitContext = Mockito.mock(GitContext.class);
    context.setGitContext(gitContext);

    // act + assert
    assertThat(new Git(context).getToolRepository()).isSameAs(context.getDefaultToolRepository());
    Mockito.verifyNoInteractions(gitContext);
  }

  /**
   * Verifies that the latest installer is not used on Linux and macOS where Git is installed via the native package manager.
   */
  @Test
  void testGetToolRepositoryOnNonWindows() {

    // arrange
    IdeTestContext macContext = freshWindowsContext();
    macContext.setSystemInfo(SystemInfoMock.MAC_X64);
    IdeTestContext linuxContext = freshWindowsContext();
    linuxContext.setSystemInfo(SystemInfoMock.LINUX_X64);

    // act + assert
    assertThat(new Git(macContext).getToolRepository()).isSameAs(macContext.getDefaultToolRepository());
    assertThat(new Git(linuxContext).getToolRepository()).isSameAs(linuxContext.getDefaultToolRepository());
  }

  /**
   * Verifies that on a fresh Windows machine where neither Git nor a Bash (Git Bash) installation is available yet, {@link GitContextImpl#findGit()} reports
   * "Git not found" by returning {@code null} instead of failing.
   */
  @Test
  void testFindGitReturnsNullOnFreshWindowsMachineWithoutGitAndBash() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    GitContext gitContext = new GitContextImpl(context);

    // act
    Path gitPath = gitContext.findGit();

    // assert
    assertThat(gitPath).as("findGit() must return null (not throw) when neither git nor bash is available").isNull();
  }

  /**
   * Verifies that a missing bash is only logged on debug level, as every caller of {@code findBash()} reports it in its own context if it is relevant.
   */
  @Test
  void testFindBashDoesNotSpamLogOnFreshWindowsMachine() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();

    // act
    Path bash = context.findBash();

    // assert
    assertThat(bash).isNull();
    assertThat(context).logAtDebug().hasMessageContaining("No bash executable could be found on your system.");
    assertThat(context).logAtInfo().hasNoMessageContaining("bash");
    assertThat(context).logAtWarning().hasNoMessageContaining("bash");
    assertThat(context).logAtError().hasNoMessageContaining("bash");
  }

  /**
   * Verifies the entire installation on a fresh Windows machine: the installer of the latest version is started with the installation wizard and awaited
   * without bash, the regular repository (that would require the URL metadata and hence Git) is never accessed and the tool is not run afterwards, as it is
   * only available in a new terminal.
   */
  @Test
  void testInstallOnFreshWindowsMachine() {

    // arrange
    IdeTestContext context = windowsContextWithoutGitInPath();
    ToolRepository defaultRepository = Mockito.mock(ToolRepository.class);
    context.setDefaultToolRepository(defaultRepository);
    VersionIdentifier latestVersion = VersionIdentifier.of("2.56.0.4");
    Path installer = context.getUserHome().resolve("git-installer.exe");
    ToolRepository latestInstallerRepository = Mockito.mock(ToolRepository.class);
    Mockito.when(latestInstallerRepository.resolveVersion(Mockito.eq("git"), Mockito.eq("git"), Mockito.any(), Mockito.any())).thenReturn(latestVersion);
    Mockito.when(latestInstallerRepository.download(Mockito.eq("git"), Mockito.eq("git"), Mockito.eq(latestVersion), Mockito.any())).thenReturn(installer);
    ProcessContext processContext = Mockito.mock(ProcessContext.class, Mockito.RETURNS_SELF);
    Mockito.when(processContext.run(ProcessMode.DEFAULT))
        .thenReturn(new ProcessResultImpl("installer", "installer", ProcessResult.SUCCESS, true, List.of()));
    context.setProcessContext(processContext);
    Git git = gitWithLatestInstallerRepository(context, latestInstallerRepository);
    ToolInstallRequest request = ToolInstallRequest.ofDirect();
    request.setRequested(new ToolEditionAndVersion(VersionIdentifier.LATEST));

    // act
    ToolInstallation installation = git.install(request);

    // assert
    assertThat(installation.resolvedVersion()).isEqualTo(latestVersion);
    assertThat(installation.installedAsynchronously()).isTrue();
    Mockito.verify(processContext).executable(installer);
    Mockito.verify(processContext).addArgs(List.of("/NORESTART", "/NOCANCEL", "/SP-"));
    Mockito.verifyNoInteractions(defaultRepository);
  }

  /**
   * Verifies that the latest version is determined from the redirect of the latest release and the installer for x64 is derived from its tag.
   *
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFetchLatestInstallerOnWindowsX64(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubLatestRelease(TAG);
    Git git = gitWithReleasesUrl(contextWithNetwork(wmRuntimeInfo, SystemInfoMock.WINDOWS_X64), wmRuntimeInfo);

    // act
    CustomToolMetadata installer = git.fetchLatestInstaller();

    // assert
    assertThat(installer.getVersion()).isEqualTo(VersionIdentifier.of("2.56.0.4"));
    assertThat(installer.getUrls()).containsExactly(releasesUrl(wmRuntimeInfo) + "/download/v2.56.0.windows.4/Git-2.56.0.4-64-bit.exe");
    assertThat(installer.getTool()).isEqualTo("git");
    assertThat(installer.getEdition()).isEqualTo("git");
    assertThat(installer.getOs()).isEqualTo(OperatingSystem.WINDOWS);
    assertThat(installer.getArch()).isEqualTo(SystemArchitecture.X64);
    assertThat(installer.getChecksums()).isNull();
  }

  /**
   * Verifies that on ARM64 the ARM64 installer is used instead of the x64 installer.
   *
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFetchLatestInstallerOnWindowsArm64(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubLatestRelease(TAG);
    Git git = gitWithReleasesUrl(contextWithNetwork(wmRuntimeInfo, SystemInfoMock.WINDOWS_ARM64), wmRuntimeInfo);

    // act
    CustomToolMetadata installer = git.fetchLatestInstaller();

    // assert
    assertThat(installer.getUrls()).containsExactly(releasesUrl(wmRuntimeInfo) + "/download/v2.56.0.windows.4/Git-2.56.0.4-arm64.exe");
    assertThat(installer.getArch()).isEqualTo(SystemArchitecture.ARM64);
  }

  /**
   * Verifies the first Git for Windows release of a Git version: its installer has no revision in the file name, but the version is the same as in the URL
   * metadata generated by the GitUrlUpdater.
   *
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFetchLatestInstallerOfFirstRelease(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubLatestRelease("v2.57.0.windows.1");
    Git git = gitWithReleasesUrl(contextWithNetwork(wmRuntimeInfo, SystemInfoMock.WINDOWS_X64), wmRuntimeInfo);

    // act
    CustomToolMetadata installer = git.fetchLatestInstaller();

    // assert
    assertThat(installer.getVersion()).isEqualTo(VersionIdentifier.of("2.57.0.1"));
    assertThat(installer.getUrls()).containsExactly(releasesUrl(wmRuntimeInfo) + "/download/v2.57.0.windows.1/Git-2.57.0-64-bit.exe");
  }

  /**
   * Verifies that a tag that is not a regular Git for Windows release is rejected.
   *
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFetchLatestInstallerRejectsUnexpectedTag(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubLatestRelease("v2.57.0-rc0.windows.1");
    Git git = gitWithReleasesUrl(contextWithNetwork(wmRuntimeInfo, SystemInfoMock.WINDOWS_X64), wmRuntimeInfo);

    // act + assert
    assertThatThrownBy(git::fetchLatestInstaller).isInstanceOf(CliException.class).hasMessageContaining("unexpected latest Git release");
  }

  /**
   * Verifies the real {@link ToolRepository} of a fresh Windows machine: it resolves the latest version, requests the latest release only once and rejects any
   * other version, since only the latest installer is known.
   *
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testLatestInstallerRepository(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubLatestRelease(TAG);
    IdeTestContext context = contextWithNetwork(wmRuntimeInfo, SystemInfoMock.WINDOWS_X64);
    context.getFileAccess().delete(context.getUrlsPath());
    Git git = gitWithReleasesUrl(context, wmRuntimeInfo);

    // act
    VersionIdentifier version = git.getToolRepository().resolveVersion("git", "git", VersionIdentifier.LATEST, git);

    // assert
    assertThat(version).isEqualTo(VersionIdentifier.of("2.56.0.4"));
    assertThat(git.getToolRepository().findSecurity("git", "git")).isSameAs(ToolSecurity.getEmpty());
    VersionIdentifier otherVersion = VersionIdentifier.of("2.40.0");
    assertThatThrownBy(() -> git.getToolRepository().resolveVersion("git", "git", otherVersion, git)).isInstanceOf(IllegalStateException.class);
    verify(1, getRequestedFor(urlEqualTo(LATEST_RELEASE_PATH)));
  }

  private static Git gitWithLatestInstallerRepository(IdeTestContext context, ToolRepository latestInstallerRepository) {

    return new Git(context) {
      @Override
      protected ToolRepository createLatestInstallerRepository() {
        return latestInstallerRepository;
      }
    };
  }

  private static Git gitWithReleasesUrl(IdeTestContext context, WireMockRuntimeInfo wmRuntimeInfo) {

    return new Git(context) {
      @Override
      protected String getReleasesUrl() {
        return releasesUrl(wmRuntimeInfo);
      }
    };
  }

  private static String releasesUrl(WireMockRuntimeInfo wmRuntimeInfo) {

    return wmRuntimeInfo.getHttpBaseUrl() + "/releases";
  }

  private static IdeTestContext contextWithNetwork(WireMockRuntimeInfo wmRuntimeInfo, SystemInfo systemInfo) {

    IdeTestContext context = newContext(PROJECT_BASIC, wmRuntimeInfo);
    context.setSystemInfo(systemInfo);
    context.getNetworkStatus().simulateOnline();
    return context;
  }

  private static void stubLatestRelease(String tag) {

    String tagPath = "/releases/tag/" + tag;
    stubFor(get(urlEqualTo(LATEST_RELEASE_PATH)).willReturn(temporaryRedirect(tagPath)));
    stubFor(get(urlEqualTo(tagPath)).willReturn(aResponse().withStatus(200)));
  }
}
