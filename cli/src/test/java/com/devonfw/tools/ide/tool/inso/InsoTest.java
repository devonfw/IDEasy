package com.devonfw.tools.ide.tool.inso;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
public class InsoTest extends AbstractIdeContextTest {

  private static final String INSO_PROJECT = "inso";

  private static final String INSO_VERSION = "12.5.0";

  @Test
  void testInsoInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(INSO_PROJECT, wireMockRuntimeInfo);
    Inso inso = new Inso(context);

    // act
    inso.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testInsoRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(INSO_PROJECT, wireMockRuntimeInfo);
    Inso inso = new Inso(context);
    inso.arguments.addValue("hello");
    inso.arguments.addValue("world");

    // act
    inso.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("inso hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("inso/.ide.software.version")).exists().hasContent(INSO_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed inso in version " + INSO_VERSION);
  }


}
