package com.devonfw.tools.ide.tool.npm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
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
   * @param os Operating system
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNpmInstall(String os, WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Npm commandlet = new Npm(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if npm can be run properly.
   *
   * @param os Operating System.
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNpmRun(String os, WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Npm commandlet = new Npm(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("npm " + getOs(context) + " --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessage("Successfully installed npm in version 9.9.2");
  }

  private String getOs(IdeTestContext context) {
    if (context.getSystemInfo().isWindows()) {
      return "windows";
    } else if (context.getSystemInfo().isLinux()) {
      return "linux";
    } else if (context.getSystemInfo().isMac()) {
      return "mac";
    }
    return "";
  }
}
