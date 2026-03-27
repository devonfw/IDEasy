package com.devonfw.tools.ide.tool.go;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Go}.
 */
@WireMockTest
class GoTest extends AbstractIdeContextTest {

  private static final String PROJECT_GO = "go";

  private static final String GO_VERSION = "1.22.4";

  @Test
  void testGoInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO, wireMockRuntimeInfo);
    Go go = new Go(context);

    // act
    go.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testGoRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO, wireMockRuntimeInfo);
    Go go = new Go(context);
    go.arguments.addValue("hello");
    go.arguments.addValue("world");

    // act
    go.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("go hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("go/.ide.software.version")).exists().hasContent(GO_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed go in version " + GO_VERSION);
  }
}
