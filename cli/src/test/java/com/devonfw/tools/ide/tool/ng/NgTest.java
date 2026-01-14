package com.devonfw.tools.ide.tool.ng;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Ng}.
 */
@WireMockTest
class NgTest extends AbstractIdeContextTest {

  private static final String PROJECT_NG = "ng";

  /**
   * Tests if the {@link Ng} install works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNgInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
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
  void testNgUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
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
  void testNgRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    Ng commandlet = new Ng(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("ng --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf @angular/cli@18.0.1");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }

}
