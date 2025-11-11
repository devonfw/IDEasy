package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link StatusCommandlet}.
 */
public class StatusCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  @Test
  public void testStatusOutsideOfHome() {
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
  public void testStatusWhenOfflineMode() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.getStartContext().setOfflineMode(true);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("You are offline because you have enabled offline mode via CLI option."),
        IdeLogEntry.ofSuccess("Your version of IDEasy is SNAPSHOT."),
        IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."));
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run without internet connection.
   */
  @Test
  public void testStatusWhenOffline() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    Exception error = context.getNetworkStatus().simulateNetworkError();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(new IdeLogEntry(IdeLogLevel.ERROR, "You are offline because of the following error:", null, null, error, false),
        IdeLogEntry.ofInteraction("Please check potential proxy settings, ensure you are properly connected to the internet and retry this operation."),
        IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."));
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run with TLS issue.
   */
  @Test
  public void testStatusWhenTlsIssue() throws Exception {

    // arrange
    IdeTestContext context = new IdeTestContext();
    SSLHandshakeException error = new SSLHandshakeException(
        "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target");
    context.getNetworkStatus().getOnlineCheck().set(error);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).log().hasEntries(new IdeLogEntry(IdeLogLevel.ERROR, "You are offline because of the following error:", null, null, error, false),
        IdeLogEntry.ofWarning(
            "You are having TLS issues. We guess you are forced to use a VPN tool breaking end-to-end encryption causing this effect. As a workaround you can call the following command:"),
        IdeLogEntry.ofInteraction("ide fix-vpn-tls-problem"),
        IdeLogEntry.ofWarning("Skipping check for newer version of IDEasy because you are offline."));
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
  public void testStatusWhenInPrivacyMode(String os, Path ideHome, Path ideRoot, Path userHome) {
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
        Arguments.of(
            "linux",
            Path.of("/mnt/c/Users/testuser/projects/myproject"),
            Path.of("/mnt/c/Users/testuser/projects"),
            Path.of("/mnt/c/projects")
        ),
        Arguments.of(
            "windows",
            Path.of("C:\\Users\\testuser\\projects\\myproject"),
            Path.of("C:\\Users\\testuser\\projects"),
            Path.of("C:\\Users\\testuser")
        ),
        Arguments.of(
            "linux",
            Path.of("/home/testuser/projects/myproject"),
            Path.of("/home/testuser/projects"),
            Path.of("/home/testuser")
        ),
        Arguments.of(
            "mac",
            Path.of("/Users/testuser/projects/myproject"),
            Path.of("/Users/testuser/projects"),
            Path.of("/Users/testuser")
        )
    );
  }
}
