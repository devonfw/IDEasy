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
    assertThat(context).logAtInfo().hasMessage("9.9.2");
  }

  /**
   * Tests if the {@link Npm} uninstall works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNpmUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasNoMessageContaining("npm uninstall -g npm");
    assertThat(context).logAtInfo().hasMessageContaining("IDEasy does not support uninstalling the tool npm since this will break your installation.\n"
        + "If you really want to uninstall it, please uninstall its parent tool via:\n"
        + "ide uninstall node");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled npm");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessage("Successfully installed npm in version 9.9.2");
    assertThat(context).logAtSuccess().hasMessageContaining("Setting npm config prefix to: " + context.getSoftwarePath().resolve("node") + " was successful");
  }
}
