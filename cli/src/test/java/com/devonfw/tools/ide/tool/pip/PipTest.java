package com.devonfw.tools.ide.tool.pip;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Pip}.
 */
@WireMockTest
public class PipTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /**
   * Tests that the {@link Pip} commandlet can be installed (delegates to UV).
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPipInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    Pip commandlet = new Pip(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testPipRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    Pip commandlet = new Pip(context);

    // act
    commandlet.run();

    // assert
    checkInstallation(context);
    assertThat(context).logAtInfo().hasMessage("pip was run");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed uv");
    // Pip delegates to UV, so we check that UV was installed
    assertThat(context.getSoftwarePath().resolve("uv")).exists();
  }
}
