package com.devonfw.tools.ide.tool.spyder;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Spyder}.
 */
@WireMockTest
class SpyderTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /**
   * Tests that the {@link Spyder} commandlet can be installed via the pip-based installation logic.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSpyderInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Spyder commandlet = new Spyder(context);

    // act
    commandlet.install();

    // assert
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed spyder");
  }

  /**
   * Tests that {@link Spyder} is implemented as {@link PipBasedCommandlet}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSpyderIsPipBasedCommandlet(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);

    // act
    Spyder commandlet = new Spyder(context);

    // assert
    assertThat(commandlet).isInstanceOf(PipBasedCommandlet.class);
  }
}
