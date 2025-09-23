package com.devonfw.tools.ide.tool.ng;

import static com.devonfw.tools.ide.context.IdeTestContext.readAndResolve;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Ng}.
 */
@WireMockTest
public class NgTest extends AbstractIdeContextTest {

  private static final String PROJECT_NG = "ng";

  private static final Path PATH_INTEGRATION_TEST = Path.of("src/test/resources/ide-projects");

  private static String ngVersion;

  /**
   * Creates a ng-version json file based on the given test resource in a temporary directory according to the http url and port of the
   * {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException on error.
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = PATH_INTEGRATION_TEST.resolve(PROJECT_NG);
    ngVersion = readAndResolve(testDataPath.resolve("ng-version.json"), wmRuntimeInfo);
  }

  /**
   * Tests if the {@link Ng} install works correctly across all three operating systems.
   *
   * @param os Operating system
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgInstall(String os, WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} uninstall works correctly across all three operating systems.
   *
   * @param os Operating system
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgUninstall(String os, WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm " + getOs(context) + " uninstall -g @angular/cli");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled ng");
  }

  /**
   * Tests if {@link Ng} run works correctly across all three operating systems.
   *
   * @param os Operating system
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgRun(String os, WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("ng " + getOs(context) + " --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm " + getOs(context) + " install -g @angular/cli@18.0.1");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }

  private String getOs(IdeTestContext context) {
    if (context.getSystemInfo().isWindows()) {
      return "windows";
    } else if (context.getSystemInfo().isLinux()) {
      return "linux";
    } else if (context.getSystemInfo().isMac()) {
      return "mac";
    }
    return "";
  }

}
