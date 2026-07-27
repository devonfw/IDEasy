package com.devonfw.tools.ide.tool.just;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.uv.UvBasedCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Just}.
 */
@WireMockTest
public class JustTest extends AbstractIdeContextTest {

  private static final String PROJECT_UV = "uv";

  /**
   * Tests that {@link Just} installs via {@code uv tool}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random part
   */
  @Test
  void testJustInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Just commandlet = new Just(context);

    // act
    commandlet.install();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("uv tool install --force rust-just@");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed just");
  }

  /**
   * Tests that {@link Just} is implemented as a {@link com.devonfw.tools.ide.tool.uv.UvBasedCommandlet}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testJustIsUvBasedCommandlet(WireMockRuntimeInfo wireMockRuntimeInfo) {

    //arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);

    // act
    Just commandlet = new Just(context);

    // assert
    assertThat(commandlet).isInstanceOf(UvBasedCommandlet.class);
  }

  /**
   * Tests that {@link Just#getInstalledVersion()} parses the version from {@code uv tool list} output.
   */
  @Test
  void testJustGetInstalledVersion(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Just commandlet = new Just(context);
    commandlet.install();

    // act
    VersionIdentifier installedVersion = commandlet.getInstalledVersion();

    // assert
    assertThat(installedVersion).isEqualTo(VersionIdentifier.of("1.37.0"));
  }
}
