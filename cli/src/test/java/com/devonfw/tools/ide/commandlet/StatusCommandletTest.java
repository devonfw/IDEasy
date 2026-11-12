package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextMock;
import com.devonfw.tools.ide.git.repository.RepositoryType;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link StatusCommandlet}.
 */
class StatusCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  @Test
  void testStatusOutsideOfHome() {
    //arrange
    IdeTestContext context = new IdeTestContext();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    //act
    status.run();

    //assert
    assertThat(context).logAtWarning().hasMessageContaining("You are not inside an IDE project: ");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run in enforced offline mode.
   */
  @Test
  void testStatusWhenOfflineMode() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.getStartContext().setOfflineMode(true);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Your version of IDEasy is SNAPSHOT."),
        IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."),
        IdeLogEntry.ofWarning("You are offline because you have enabled offline mode via CLI option."));
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run without internet connection.
   */
  @Test
  void testStatusWhenOffline() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    Exception error = context.getNetworkStatus().simulateNetworkError();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."),
        new IdeLogEntry(IdeLogLevel.ERROR, "You are offline because of the following error:", null, null, error, false),
        IdeLogEntry.ofInteraction("Please check potential proxy settings, ensure you are properly connected to the internet and retry this operation."));
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run with TLS issue.
   */
  @Test
  void testStatusWhenTlsIssue() throws Exception {

    // arrange
    IdeTestContext context = new IdeTestContext();
    SSLHandshakeException error = new SSLHandshakeException(
        "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target");
    context.getNetworkStatus().getOnlineCheck().set(error);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."),
        new IdeLogEntry(IdeLogLevel.ERROR, "You are offline because of the following error:", null, null, error, false));
    assertThat(context).logAtWarning().hasMessageContaining(
        "The TLS connection to https://www.github.com failed due to a certificate trust error (PKIX / certificate-path / SSL handshake).");
    assertThat(context).logAtWarning().hasMessageContaining("ide fix-vpn-tls-problem https://www.github.com");
    assertThat(context).logAtInteraction()
        .hasMessageContaining("https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc#tls-certificate-issues");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run and online-check error message contains known PKIX trust issue text.
   */
  @Test
  void testStatusWhenPkixIssueInMessage() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    IllegalStateException error = new IllegalStateException("PKIX path building failed: unable to find valid certification path to requested target");
    context.getNetworkStatus().getOnlineCheck().set(error);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtWarning().hasMessageContaining("ide fix-vpn-tls-problem https://www.github.com");
    assertThat(context).logAtInteraction().hasMessageContaining("proxy-support.adoc#tls-certificate-issues");
  }

  /**
   * Tests that {@link StatusCommandlet} reports an outdated project with a message that is understandable for end-users and does not expose the internal
   * migration target version.
   */
  @Test
  void testStatusWhenProjectNeedsMigration() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setProjectVersion(VersionIdentifier.ofRequired("2025.01.001-beta"));
    VersionIdentifier targetVersion = new IdeMigrator().getTargetVersion();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtInteraction().hasMessage("Your project '" + context.getProjectName()
        + "' was created or last updated with IDEasy version 2025.01.001-beta and needs an update.\nPlease run 'ide update' to migrate your project.");
    assertThat(context).log().hasNoMessageContaining(targetVersion.toString());
  }

  /**
   * Tests that {@link StatusCommandlet} does not report a required project update if the project is already up-to-date.
   */
  @Test
  void testStatusWhenProjectIsUpToDate() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setProjectVersion(new IdeMigrator().getTargetVersion());
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasNoMessageContaining("needs an update");
  }

  /**
   * Tests that {@link StatusCommandlet} reports the settings as up-to-date for a combined code and settings repository if no settings update is available.
   */
  @Test
  void testStatusWhenSettingsAreUpToDateForCombinedCodeSettingsRepository(@TempDir Path tempDir) throws IOException {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();

    // arrange, a context whose settings folder is a symlink into a combined code and settings repository
    IdeTestContext context = newContext(PROJECT_BASIC);
    makeCombinedCodeSettingsRepository(context, tempDir);

    // act
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);
    status.run();

    // assert
    assertThat(context).logAtSuccess().hasMessage("Your settings are up-to-date.");
    assertThat(context).logAtWarning().hasNoMessageContaining("not up-to-date");
  }

  /**
   * Tests that {@link StatusCommandlet} suppresses the outdated settings warning for a combined code and settings repository even if an update is available
   * (the combined repository is not updated by {@code ide update}).
   */
  @Test
  void testStatusDoesNotWarnAboutOutdatedSettingsForCombinedCodeSettingsRepository(@TempDir Path tempDir) throws IOException {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    makeCombinedCodeSettingsRepository(context, tempDir);
    // the update check must see a pending update to prove that the warning is really suppressed for the combined repository
    context.setGitContext(new GitContextMock(context) {
      @Override
      public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

        return true;
      }
    });

    // act
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);
    status.run();

    // assert
    assertThat(context).logAtWarning().hasNoMessageContaining("not up-to-date");
    assertThat(context).logAtSuccess().hasNoMessageContaining("Your settings are up-to-date.");
  }

  /**
   * Turns the given context into one whose IDE_HOME settings folder is a real symbolic link to the {@code settings} folder of a combined code and settings
   * repository (a git repository that itself contains the settings folder), as created by {@code ide create --code-settings-repository}. The network is
   * made online and the project up-to-date so the settings git status is the only relevant part of the status output.
   *
   * @param context the context to adapt.
   * @param tempDir a temporary directory the combined repository is created in.
   * @throws IOException if the fixture cannot be created.
   */
  private void makeCombinedCodeSettingsRepository(IdeTestContext context, Path tempDir) throws IOException {

    // a git repository that contains a settings folder (a combined code and settings repository)
    Path repository = tempDir.resolve("combined-repo");
    Files.createDirectories(repository.resolve(IdeContext.FOLDER_SETTINGS));
    Files.createFile(repository.resolve(IdeContext.FOLDER_SETTINGS).resolve(EnvironmentVariables.DEFAULT_PROPERTIES));
    Files.createDirectory(repository.resolve(GitContext.GIT_FOLDER));
    Files.writeString(repository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD), "ref: refs/heads/main");

    // replace the settings folder of the IDE_HOME with a symlink into the combined repository
    Path settingsLink = context.getIdeHome().resolve(IdeContext.FOLDER_SETTINGS);
    context.getFileAccess().delete(settingsLink);
    context.getFileAccess().symlink(repository.resolve(IdeContext.FOLDER_SETTINGS), settingsLink, false);
    context.setSettingsPath(settingsLink);

    context.getNetworkStatus().simulateOnline();
    context.setProjectVersion(new IdeMigrator().getTargetVersion());
    // sanity check: the fixture must actually be detected as a combined code and settings repository
    assertThat(RepositoryType.ofSettingsPath(context.getSettingsPath(), context)).isEqualTo(RepositoryType.CODE_SETTINGS_COMBINED);
  }

  /**
   * Tests that the output of {@link StatusCommandlet} does not contain the username when run with active privacy mode on all OS (windows, linux, WSL, mac).
   *
   * @param os the operating system to test on.
   * @param ideHome the path to the IDE home.
   * @param ideRoot the path to IDE root.
   * @param userHome the path to the user home.
   */
  @ParameterizedTest
  @MethodSource("providePrivacyModeTestCases")
  void testStatusWhenInPrivacyMode(String os, Path ideHome, Path ideRoot, Path userHome) {
    // arrange
    IdeTestContext context = new IdeTestContext();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    context.setUserHome(userHome);
    context.setIdeHome(ideHome);
    context.setIdeRoot(ideRoot);
    context.getStartContext().setPrivacyMode(true);

    // act
    status.run();

    // assert
    assertThat(context).log().hasNoMessageContaining("testuser");
    assertThat(context).logAtInfo().hasMessageContaining("Your operating system is " + os);
  }

  private static Stream<Arguments> providePrivacyModeTestCases() {
    return Stream.of(
        Arguments.of("linux", Path.of("/mnt/c/Users/testuser/projects/myproject"), Path.of("/mnt/c/Users/testuser/projects"), Path.of("/mnt/c/projects")),
        Arguments.of("windows", Path.of("C:\\Users\\testuser\\projects\\myproject"), Path.of("C:\\Users\\testuser\\projects"), Path.of("C:\\Users\\testuser")),
        Arguments.of("linux", Path.of("/home/testuser/projects/myproject"), Path.of("/home/testuser/projects"), Path.of("/home/testuser")),
        Arguments.of("mac", Path.of("/Users/testuser/projects/myproject"), Path.of("/Users/testuser/projects"), Path.of("/Users/testuser")));
  }
}
