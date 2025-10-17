package com.devonfw.tools.ide.tool.ng;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Ng}.
 */
@WireMockTest
public class NgTest extends AbstractIdeContextTest {

  private static final String PROJECT_NG = "ng";

  /**
   * Tests if the {@link Ng} install works correctly on windows (temporarily disabled until file permission bug is fixed). Check:
   * https://github.com/devonfw/IDEasy/issues/1509
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  @Disabled
  public void testNgInstallWindows(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} install works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} uninstall works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    Ng commandlet = new Ng(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g @angular/cli");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled ng");
  }

  /**
   * Tests if {@link Ng} run works correctly on linux.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testNgRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG, wireMockRuntimeInfo);
    Ng commandlet = new Ng(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("ng --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -g @angular/cli@18.0.1");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }

}
