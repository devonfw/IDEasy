package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Integration test of {@link InstallCommandlet}.
 */

public class InstallCommandletTest extends AbstractIdeContextTest {

  private static WireMockServer server;

  private static Path resourcePath = Path.of("src/test/resources");

  @BeforeAll
  static void setUp() throws IOException {

    server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(1111));
    server.start();
  }

  @AfterAll
  static void tearDown() throws IOException {

    server.shutdownServer();
  }

  private void mockWebServer() throws IOException {

    Path windowsFilePath = resourcePath.resolve("__files").resolve("java-17.0.6-windows-x64.zip");
    String windowsLength = String.valueOf(Files.size(windowsFilePath));
    server.stubFor(get(urlPathEqualTo("/installTest/windows")).willReturn(
        aResponse().withHeader("Content-Type", "application/zip").withHeader("Content-Length", windowsLength)
            .withStatus(200).withBodyFile("java-17.0.6-windows-x64.zip")));

    Path linuxFilePath = resourcePath.resolve("__files").resolve("java-17.0.6-linux-x64.tgz");
    String linuxLength = String.valueOf(Files.size(linuxFilePath));
    server.stubFor(get(urlPathEqualTo("/installTest/linux")).willReturn(
        aResponse().withHeader("Content-Type", "application/tgz").withHeader("Content-Length", linuxLength)
            .withStatus(200).withBodyFile("java-17.0.6-linux-x64.tgz")));

    server.stubFor(get(urlPathEqualTo("/installTest/macOS")).willReturn(
        aResponse().withHeader("Content-Type", "application/tgz").withHeader("Content-Length", linuxLength)
            .withStatus(200).withBodyFile("java-17.0.6-linux-x64.tgz")));
  }

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is null.
   */
  @Test
  public void testInstallCommandletRunWithVersion() throws IOException {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    mockWebServer();
    // act
    install.run();
    // assert
    assertTestInstall(context);
  }

  /**
   * Test of {@link InstallCommandlet} run, when Installed Version is set.
   */
  @Test
  public void testInstallCommandletRunWithVersionAndVersionIdentifier() throws IOException {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("java", context);
    install.version.setValueAsString("17.0.6", context);
    mockWebServer();

    // act
    install.run();
    // assert
    assertTestInstall(context);
  }

  private void assertTestInstall(IdeContext context) {

    assertThat(context.getSoftwarePath().resolve("java")).exists();
    assertThat(context.getSoftwarePath().resolve("java/InstallTest.txt")).hasContent("This is a test file.");
    assertThat(context.getSoftwarePath().resolve("java/bin/HelloWorld.txt")).hasContent("Hello World!");
    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java.cmd")).exists();
    } else if (context.getSystemInfo().isLinux() || context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    }
  }
}
