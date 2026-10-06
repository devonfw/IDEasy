package com.devonfw.tools.ide.tool.flutter;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Flutter}.
 */
@WireMockTest
class FlutterTest extends AbstractIdeContextTest {

  private static final String PROJECT_FLUTTER = "flutter";

  private static final String FLUTTER_VERSION = "3.47.5";

  @Test
  void testFlutterInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_FLUTTER, wireMockRuntimeInfo);
    Flutter flutter = new Flutter(context);

    // act
    flutter.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testFlutterRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_FLUTTER, wireMockRuntimeInfo);
    Flutter flutter = new Flutter(context);
    flutter.arguments.addValue("hello");
    flutter.arguments.addValue("world");

    // act
    flutter.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("flutter hello world");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("flutter/.ide.software.version")).exists().hasContent(FLUTTER_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed flutter in version " + FLUTTER_VERSION);
  }
}
