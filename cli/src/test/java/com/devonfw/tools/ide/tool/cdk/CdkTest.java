package com.devonfw.tools.ide.tool.cdk;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Cdk}.
 */
@WireMockTest
class CdkTest extends AbstractIdeContextTest {

  private static final String PROJECT_CDK = "cdk";

  /**
   * Tests if the {@link Cdk} install works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCdkInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CDK, wireMockRuntimeInfo);
    Cdk commandlet = new Cdk(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);

  }

  /**
   * Tests if the {@link Cdk} install works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCdkUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CDK, wireMockRuntimeInfo);
    Cdk commandlet = new Cdk(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g aws-cdk");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled cdk");
  }

  /**
   * Tests if {@link Cdk} run works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testCdkRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CDK, wireMockRuntimeInfo);
    Cdk commandlet = new Cdk(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("cdk --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf aws-cdk@2.1120.0");

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed cdk in version 2.1120.0");
  }
}
