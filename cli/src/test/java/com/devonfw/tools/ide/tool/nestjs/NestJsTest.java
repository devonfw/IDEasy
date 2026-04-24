package com.devonfw.tools.ide.tool.nestjs;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link NestJs}.
 */
@WireMockTest
class NestJsTest extends AbstractIdeContextTest {

  private static final String PROJECT_NESTJS = "nestjs";

  /**
   * Tests if the {@link NestJs} install works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNestJsInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NESTJS, wireMockRuntimeInfo);
    NestJs commandlet = new NestJs(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);

  }

  /**
   * Tests if the {@link NestJs} install works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNestJsUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NESTJS, wireMockRuntimeInfo);
    NestJs commandlet = new NestJs(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g @nestjs/cli");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled nestjs"); //nestjs
  }

  /**
   * Tests if {@link NestJs} run works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNestJsRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NESTJS, wireMockRuntimeInfo);
    NestJs commandlet = new NestJs(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("nestjs --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf @nestjs/cli@11.0.21");

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed nestjs in version 11.0.21");
  }
}
