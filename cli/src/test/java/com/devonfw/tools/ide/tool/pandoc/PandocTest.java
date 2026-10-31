package com.devonfw.tools.ide.tool.pandoc;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Pandoc}.
 */
@WireMockTest
public class PandocTest extends AbstractIdeContextTest {

  private static final String PANDOC_PROJECT = "pandoc";

  private static final String PANDOC_VERSION = "3.12";

  @Test
  void testPandocInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PANDOC_PROJECT, wireMockRuntimeInfo);
    Pandoc pandoc = new Pandoc(context);

    // act
    pandoc.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testPandocRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PANDOC_PROJECT, wireMockRuntimeInfo);
    Pandoc pandoc = new Pandoc(context);
    pandoc.arguments.addValue("hello");
    pandoc.arguments.addValue("world");

    // act
    pandoc.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("pandoc hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("pandoc/.ide.software.version")).exists().hasContent(PANDOC_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed pandoc in version " + PANDOC_VERSION);
  }
}
