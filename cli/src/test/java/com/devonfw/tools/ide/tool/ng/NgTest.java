package com.devonfw.tools.ide.tool.ng;

import static com.devonfw.tools.ide.context.IdeTestContext.readAndResolve;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
   * Tests if the {@link Ng} install works correctly on windows (temporarily disabled until file permission bug is fixed). Check:
   * https://github.com/devonfw/IDEasy/issues/1509
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  @Disabled
  public void testNgInstallWindows(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} install works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} uninstall works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g @angular/cli");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled ng");
  }

  /**
   * Tests if {@link Ng} run works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(aResponse().withStatus(200).withBody(ngVersion)));
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("ng --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -g @angular/cli@18.0.1");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }

}
