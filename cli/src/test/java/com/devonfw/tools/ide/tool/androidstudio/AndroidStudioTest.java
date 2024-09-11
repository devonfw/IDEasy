package com.devonfw.tools.ide.tool.androidstudio;

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
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for {@link AndroidStudio Android Studio IDE} tests.
 */
@WireMockTest
public class AndroidStudioTest extends AbstractIdeContextTest {

  private static final String ANDROID_STUDIO = "android-studio";
  private static final String MOCKED_PLUGIN_JAR = "mocked-plugin.jar";

  private final IdeTestContext context = newContext(ANDROID_STUDIO);

  /**
   * Tests if {@link AndroidStudio Android Studio IDE} can be installed.
   *
   * @param os String of the OS to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testAndroidStudioInstall(String os, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    setupMockedPlugin(wmRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    AndroidStudio commandlet = new AndroidStudio(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
  }

  /**
   * Tests if {@link AndroidStudio Android Studio IDE} can be run.
   *
   * @param os String of the OS to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testAndroidStudioRun(String os, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    setupMockedPlugin(wmRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    AndroidStudio commandlet = new AndroidStudio(this.context);

    // act
    commandlet.run();

    // assert
    assertThat(this.context).logAtInfo().hasMessage(ANDROID_STUDIO + " " + this.context.getSystemInfo().getOs() + " " + this.context.getWorkspacePath());

    checkInstallation(this.context);
  }

  private void checkInstallation(IdeTestContext context) {
    // commandlet - android-studio
    assertThat(context.getSoftwarePath().resolve("android-studio/.ide.software.version")).exists().hasContent("2024.1.1.1");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed android-studio in version 2024.1.1.1");
    assertThat(context).logAtSuccess().hasMessage("Install plugin: mockedPlugin");
    assertThat(context.getPluginsPath().resolve("android-studio").resolve("mockedPlugin").resolve("MockedClass.class")).exists();
  }

  private void setupMockedPlugin(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    String content = "plugin_id=mockedPlugin\nplugin_active=true\nplugin_url=" + wmRuntimeInfo.getHttpBaseUrl() + "/mockedPlugin";
    Files.write(this.context.getSettingsPath().resolve("android-studio").resolve("plugins").resolve("MockedPlugin.properties"),
        content.getBytes(StandardCharsets.UTF_8));

    Path mockedPlugin = this.context.getIdeRoot().resolve("repository").resolve(MOCKED_PLUGIN_JAR);
    byte[] contentBytes = Files.readAllBytes(mockedPlugin);
    int contentLength = contentBytes.length;

    stubFor(any(urlEqualTo("/mockedPlugin")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withHeader("Content-Length", String.valueOf(contentLength))
            .withBody(contentBytes)));
  }
}
