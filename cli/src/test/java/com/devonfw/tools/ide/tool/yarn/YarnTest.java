package com.devonfw.tools.ide.tool.yarn;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.node.Node;
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
    Yarn yarn = context.getCommandletManager().getCommandlet(Yarn.class);

    // act
    yarn.install();

    // assert
    checkInstallation(context);
  }

  @Test
  void testYarnInstallWhenNodeInstalled(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN, wireMockRuntimeInfo);
    Node node = context.getCommandletManager().getCommandlet(Node.class);
    Yarn yarn = context.getCommandletManager().getCommandlet(Yarn.class);

    // act
    node.install();
    yarn.install();

    // assert
    assertThat(context).logAtDebug()
        .hasMessageContaining("npm' using bash with arguments 'list' '-g' 'yarn' '--depth=0'"); // since npm was installed this should be called
    assertThat(context).log().hasNoMessageContaining("-- yarn@");
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
    Yarn yarn = context.getCommandletManager().getCommandlet(Yarn.class);

    // act I
    yarn.install();

    // assert I
    checkInstallation(context);

    // act II
    yarn.uninstall();

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
    Yarn yarn = context.getCommandletManager().getCommandlet(Yarn.class);
    yarn.arguments.setValue("--version");

    // act
    yarn.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("yarn --version");
    assertThat(context).logAtInfo().hasMessageContaining("yarn version: 2.4.3");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessage("npm install -gf yarn@2.4.3");
    assertThat(context).logAtSuccess().hasMessageContaining("Setting npm config prefix to: " + context.getSoftwarePath().resolve("node") + " was successful");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed yarn in version 2.4.3");
  }
}
