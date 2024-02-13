package com.devonfw.tools.ide.Jmc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Integration test of {@link com.devonfw.tools.ide.tool.jmc.Jmc}.
 */
public class JmcTest extends AbstractIdeContextTest {

  private static WireMockServer server;

  private static Path resourcePath = Paths.get("src/test/resources");

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

    String windowsFilenameJmc = "org.openjdk.jmc-8.3.0-win32.win32.x86_64.zip";
    String linuxFilenameJmc = "org.openjdk.jmc-8.3.0-linux.gtk.x86_64.tar.gz";
    String macOSFilenameJmc = "org.openjdk.jmc-8.3.0-macosx.cocoa.x86_64.tar.gz";
    String windowsFilenameJava = "java-17.0.6-windows-x64.zip";
    String linuxFilenameJava = "java-17.0.6-linux-x64.tgz";
    String resourceFilesDirName = "__files";

    Path windowsFilePathJmc = resourcePath.resolve(resourceFilesDirName).resolve(windowsFilenameJmc);
    String windowsLengthJmc = String.valueOf(Files.size(windowsFilePathJmc));

    Path linuxFilePathJmc = resourcePath.resolve(resourceFilesDirName).resolve(linuxFilenameJmc);
    String linuxLengthJmc = String.valueOf(Files.size(linuxFilePathJmc));

    Path macOSFilePathJmc = resourcePath.resolve(resourceFilesDirName).resolve(macOSFilenameJmc);
    String maxOSLengthJmc = String.valueOf(Files.size(macOSFilePathJmc));

    Path windowsFilePathJava = resourcePath.resolve(resourceFilesDirName).resolve(windowsFilenameJava);
    String windowsLengthJava = String.valueOf(Files.size(windowsFilePathJava));

    Path linuxFilePathJava = resourcePath.resolve(resourceFilesDirName).resolve(linuxFilenameJava);
    String linuxLengthJava = String.valueOf(Files.size(linuxFilePathJava));

    setupMockServerResponse("/jmcTest/windows", "application/zip", windowsLengthJmc, windowsFilenameJmc);
    setupMockServerResponse("/jmcTest/linux", "application/gz", linuxLengthJmc, linuxFilenameJmc);
    setupMockServerResponse("/jmcTest/macOS", "application/gz", maxOSLengthJmc, macOSFilenameJmc);
    setupMockServerResponse("/installTest/windows", "application/zip", windowsLengthJava, windowsFilenameJava);
    setupMockServerResponse("/installTest/linux", "application/tgz", linuxLengthJava, linuxFilenameJava);
    setupMockServerResponse("/installTest/macOS", "application/tgz", linuxLengthJava, linuxFilenameJava);

  }

  private void setupMockServerResponse(String testUrl, String contentType, String contentLength, String bodyFile) {

    server.stubFor(get(urlPathEqualTo(testUrl)).willReturn(aResponse().withHeader("Content-Type", contentType)
        .withHeader("Content-Length", contentLength).withStatus(200).withBodyFile(bodyFile)));
  }

  @Test
  public void jmcPostInstallShouldMoveFilesIfRequired() throws IOException {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jmc", context);
    mockWebServer();
    // act
    install.run();

    // assert
    assertThat(context.getSoftwarePath().resolve("jmc")).exists();
    assertThat(context.getSoftwarePath().resolve("jmc/InstallTest.txt")).hasContent("This is a test file.");

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("jmc/jmc.cmd")).exists();
    } else if (context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/jmc")).exists();
    }

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/HelloWorld.txt")).hasContent("Hello World!");
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control")).doesNotExist();
    }

    if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control.app")).exists();
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control.app/Contents")).exists();
    }

  }

}