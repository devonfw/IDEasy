package com.devonfw.tools.ide.tool.mn;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Micronaut}.
 */
@WireMockTest
public class MicronautTest extends AbstractIdeContextTest {

  private static final String MN_PROJECT = "mn";

  private static final String MN_VERSION = "5.2.1";

  @Test
  void testMicronautInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MN_PROJECT, wireMockRuntimeInfo);
    Micronaut micronaut = new Micronaut(context);

    // act
    micronaut.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testMicronautRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MN_PROJECT, wireMockRuntimeInfo);
    Micronaut micronaut = new Micronaut(context);
    micronaut.arguments.addValue("create-app");
    micronaut.arguments.addValue("demo");

    // act
    micronaut.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("mn create-app demo");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("mn/.ide.software.version")).exists().hasContent(MN_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed mn in version " + MN_VERSION);
  }
}
