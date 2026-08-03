package com.devonfw.tools.ide.tool.ruff;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.uv.UvBasedCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Ruff}.
 */
@WireMockTest
public class RuffTest extends AbstractIdeContextTest {

  private static final String PROJECT_UV = "uv";

  /**
   * Tests that {@link Ruff} installs via {@code uv tool}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random part
   */
  @Test
  void testRuffInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    Ruff commandlet = new Ruff(context);

    // act
    commandlet.install();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("uv tool install --force ruff@");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed ruff");
  }

  /**
   * Tests that {@link Ruff} is implemented as a {@link UvBasedCommandlet}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testRuffIsUvBasedCommandlet(WireMockRuntimeInfo wireMockRuntimeInfo) {

    //arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);

    // act
    Ruff commandlet = new Ruff(context);

    // assert
    assertThat(commandlet).isInstanceOf(UvBasedCommandlet.class);
  }

  /**
   * Tests that {@link Ruff#getInstalledVersion()} parses the version from {@code uv tool list} output.
   */
  @Test
  void testRuffGetInstalledVersion(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    Ruff commandlet = new Ruff(context);
    commandlet.install();

    // act
    VersionIdentifier installedVersion = commandlet.getInstalledVersion();

    // assert
    assertThat(installedVersion).isEqualTo(VersionIdentifier.of("0.15.22"));
  }

}
