package com.devonfw.tools.ide.tool.copilot;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Copilot}.
 */
@WireMockTest
class CopilotTest extends AbstractIdeContextTest {

  private static final String PROJECT_COPILOT = "copilot";

  private static final String COPILOT_VERSION = "1.0.36";

  @Test
  void testCopilotInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_COPILOT, wireMockRuntimeInfo);
    Copilot copilot = new Copilot(context);

    // act
    copilot.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testCopilotRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_COPILOT, wireMockRuntimeInfo);
    Copilot copilot = new Copilot(context);
    copilot.arguments.addValue("hello");
    copilot.arguments.addValue("world");

    // act
    copilot.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("copilot hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("copilot/.ide.software.version")).exists().hasContent(COPILOT_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed copilot in version " + COPILOT_VERSION);
  }
}
