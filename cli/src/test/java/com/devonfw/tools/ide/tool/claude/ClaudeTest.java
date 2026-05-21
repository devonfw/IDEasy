package com.devonfw.tools.ide.tool.claude;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class ClaudeTest extends AbstractIdeContextTest {

  private static final String PROJECT_CLAUDE = "claude";

  private static final String CLAUDE_VERSION = "2.1.118";

  @Test
  void testClaudeInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);

    // act
    claude.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testClaudeRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);
    claude.arguments.addValue("hello");
    claude.arguments.addValue("world");

    // act
    claude.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("claude hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("claude/.ide.software.version")).exists().hasContent(CLAUDE_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed claude in version " + CLAUDE_VERSION);
  }
}
