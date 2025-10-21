package com.devonfw.tools.ide.tool.npm;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Npm}.
 */
@WireMockTest
public class NpmTest extends AbstractIdeContextTest {

  private static final String PROJECT_NPM = "npm";

  /**
   * Tests if the {@link Npm} install works correctly across all three operating systems.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNpmInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if npm can be run properly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNpmRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("npm --version");
    assertThat(context).logAtInfo().hasMessageContaining("npm version: 9.9.2");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessage("Successfully installed npm in version 9.9.2");
    assertThat(context).logAtSuccess().hasMessageContaining("Setting npm config prefix to: " + context.getSoftwarePath().resolve("node") + " was successful");
  }
}
