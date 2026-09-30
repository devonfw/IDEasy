package com.devonfw.tools.ide.tool.dart;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Dart}.
 */
@WireMockTest
class DartTest extends AbstractIdeContextTest {

  private static final String PROJECT_DART = "dart";

  private static final String DART_VERSION = "3.13.5";

  @Test
  void testDartInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_DART, wireMockRuntimeInfo);
    Dart dart = new Dart(context);

    // act
    dart.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testDartRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_DART, wireMockRuntimeInfo);
    Dart dart = new Dart(context);
    dart.arguments.addValue("hello");
    dart.arguments.addValue("world");

    // act
    dart.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("dart hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("dart/.ide.software.version")).exists().hasContent(DART_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed dart in version " + DART_VERSION);
  }
}
