package com.devonfw.tools.ide.tool.yarn;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Yarn}.
 */
@WireMockTest
class YarnTest extends AbstractIdeContextTest {

  private static final String PROJECT_YARN = "yarn";

  /**
   * Tests if the {@link Yarn} install works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testYarnInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
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
  void testYarnUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Yarn commandlet = new Yarn(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g yarn");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled yarn");
  }

  /**
   * Tests if {@link Yarn} run works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testYarnRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Yarn commandlet = new Yarn(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("yarn --version");
    assertThat(context).logAtInfo().hasMessageContaining("yarn version: 2.4.3");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf yarn@2.4.3");
    assertThat(context).logAtSuccess().hasMessageContaining("Setting npm config prefix to: " + context.getSoftwarePath().resolve("node") + " was successful");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed yarn in version 2.4.3");
  }
}
