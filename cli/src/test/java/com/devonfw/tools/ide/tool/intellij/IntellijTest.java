package com.devonfw.tools.ide.tool.intellij;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Intellij}.
 */
@WireMockTest
public class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";
  private static final String MOCKED_PLUGIN_JAR = "mocked-plugin.jar";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstall(String os, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    setupMockedPlugin(wmRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    //if tool already installed
    commandlet.install();
    assertLogMessage(this.context, IdeLogLevel.DEBUG, "Version 2023.3.3 of tool intellij is already installed");
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijRun(String os, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    setupMockedPlugin(wmRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.run();

    // assert
    SystemInfo currentSystemInfo = this.context.getSystemInfo();
    Path workspacePath = this.context.getWorkspacePath();

    if (currentSystemInfo.isMac()) {
      assertLogMessage(this.context, IdeLogLevel.INFO, "intellij mac " + workspacePath);
    } else if (currentSystemInfo.isLinux()) {
      assertLogMessage(this.context, IdeLogLevel.INFO, "intellij linux " + workspacePath);
    } else if (currentSystemInfo.isWindows()) {
      assertLogMessage(this.context, IdeLogLevel.INFO, "intellij windows " + workspacePath);
    }
    checkInstallation(this.context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context.getVariables().get("IDEA_PROPERTIES")).isEqualTo(context.getWorkspacePath().resolve("idea.properties").toString());
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Install plugin: mockedPlugin");
    assertThat(context.getPluginsPath().resolve("intellij").resolve("mockedPlugin").resolve("MockedClass.class")).exists();
  }

  private void setupMockedPlugin(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    String content = "plugin_id=mockedPlugin\nplugin_active=true\nplugin_url=" + wmRuntimeInfo.getHttpBaseUrl() + "/mockedPlugin";
    Files.write(this.context.getSettingsPath().resolve("intellij").resolve("plugins").resolve("MockedPlugin.properties"),
        content.getBytes(StandardCharsets.UTF_8));

    Path mockedPlugin = this.context.getIdeRoot().resolve("repository").resolve(MOCKED_PLUGIN_JAR);
    byte[] contentBytes = Files.readAllBytes(mockedPlugin);
    int contentLength = contentBytes.length;

    stubFor(any(urlEqualTo("/mockedPlugin")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withHeader("Content-Length", String.valueOf(contentLength))
            .withBody(contentBytes)));
  }

}
