package com.devonfw.tools.ide.tool.dart;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Dart}.
 */
@WireMockTest
class DartTest extends AbstractIdeContextTest {

  private static final String PROJECT_DART = "dart";

  private static final String DART_VERSION = "3.13.5";

  /**
   * Provides every platform the Dart SDK is published for (see {@code DartUrlUpdater} which generates the corresponding {@code «os»_«arch».urls}
   * files), so we can verify that the platform-specific download URL is selected and the tool installs on each of them.
   *
   * @return a {@link Stream} of {@link Arguments}, one per supported platform (a display label and the matching {@link SystemInfo}).
   */
  static Stream<Arguments> platforms() {

    return Stream.of(
        Arguments.of("linux-x64", SystemInfoMock.LINUX_X64),
        // no LINUX_ARM64 constant in SystemInfoMock, so construct the mock inline
        Arguments.of("linux-arm64", new SystemInfoImpl("Linux", "6.8.0-generic", "arm64")),
        Arguments.of("mac-x64", SystemInfoMock.MAC_X64),
        Arguments.of("mac-arm64", SystemInfoMock.MAC_ARM64),
        Arguments.of("windows-x64", SystemInfoMock.WINDOWS_X64));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("platforms")
  void testDartInstallSucceedsOnAllPlatformsViaHttpDownload(String platform, SystemInfo systemInfo,
      WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_DART, wireMockRuntimeInfo);
    context.setSystemInfo(systemInfo);
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
