package com.devonfw.tools.ide.tool.yarn;

import static com.devonfw.tools.ide.context.IdeTestContext.readAndResolve;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Yarn}.
 */
@WireMockTest
public class YarnTest extends AbstractIdeContextTest {

  private static final String PROJECT_YARN = "yarn";

  private static String yarnVersion;
  private static String corepackVersion;

  /**
   * Creates a yarn-version and corepack-version json file based on the given test resource in a temporary directory according to the http url and port of the
   * {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException on error.
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = TEST_PROJECTS.resolve(PROJECT_YARN);
    yarnVersion = readAndResolve(testDataPath.resolve("yarn-version.json"), wmRuntimeInfo);
    corepackVersion = readAndResolve(testDataPath.resolve("corepack-version.json"), wmRuntimeInfo);
  }

  /**
   * Tests if the {@link Yarn} install works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testYarnInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/yarn")).willReturn(aResponse().withStatus(200).withBody(yarnVersion)));
    stubFor(get(urlMatching("/corepack")).willReturn(aResponse().withStatus(200).withBody(corepackVersion)));
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Yarn commandlet = new Yarn(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Yarn} uninstall works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testYarnUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/yarn")).willReturn(aResponse().withStatus(200).withBody(yarnVersion)));
    stubFor(get(urlMatching("/corepack")).willReturn(aResponse().withStatus(200).withBody(corepackVersion)));
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Yarn commandlet = new Yarn(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("corepack disable yarn");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled yarn");
  }

  /**
   * Tests if {@link Yarn} run works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testYarnRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/yarn")).willReturn(aResponse().withStatus(200).withBody(yarnVersion)));
    stubFor(get(urlMatching("/corepack")).willReturn(aResponse().withStatus(200).withBody(corepackVersion)));
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Yarn commandlet = new Yarn(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("yarn --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -g corepack@0.34.0");
    assertThat(context).logAtInfo().hasMessageContaining("corepack prepare yarn@2.4.3 --activate");
    assertThat(context).logAtInfo().hasMessageContaining("corepack install -g yarn@2.4.3");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed yarn in version 2.4.3");
  }
}
