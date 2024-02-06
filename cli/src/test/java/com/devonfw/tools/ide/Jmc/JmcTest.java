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

    String windowsFilename = "org.openjdk.jmc-8.3.0-win32.win32.x86_64.zip";
    String linuxFilename = "org.openjdk.jmc-8.3.0-linux.gtk.x86_64.tar.gz";
    String macOSFilename = "org.openjdk.jmc-8.3.0-macosx.cocoa.x86_64.tar.gz";

    Path windowsFilePath = resourcePath.resolve("__files").resolve(windowsFilename);

    String windowsLength = String.valueOf(Files.size(windowsFilePath));
    server.stubFor(
        get(urlPathEqualTo("/installTest/windows")).willReturn(aResponse().withHeader("Content-Type", "application/zip")
            .withHeader("Content-Length", windowsLength).withStatus(200).withBodyFile(windowsFilename)));

    Path linuxFilePath = resourcePath.resolve("__files").resolve(linuxFilename);
    String linuxLength = String.valueOf(Files.size(linuxFilePath));
    server.stubFor(
        get(urlPathEqualTo("/installTest/linux")).willReturn(aResponse().withHeader("Content-Type", "application/gz")
            .withHeader("Content-Length", linuxLength).withStatus(200).withBodyFile(linuxFilename)));

    Path macOSFilePath = resourcePath.resolve("__files").resolve(macOSFilename);
    String maxOSLength = String.valueOf(Files.size(macOSFilePath));
    server.stubFor(
        get(urlPathEqualTo("/installTest/macOS")).willReturn(aResponse().withHeader("Content-Type", "application/gz")
            .withHeader("Content-Length", maxOSLength).withStatus(200).withBodyFile(macOSFilename)));

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