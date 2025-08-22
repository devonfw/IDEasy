package com.devonfw.tools.ide.tool.ng;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Integration test of {@link Ng}.
 */
public class NgTest extends AbstractIdeContextTest {

  private static final String PROJECT_NG = "ng";

  /**
   * Tests if the {@link Ng} install works correctly across all three operating systems.
   *
   * @param os Operating system
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgInstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Ng} uninstall works correctly across all three operating systems.
   *
   * @param os Operating system
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgUninstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm" + getNpmBinaryName(context) + " " + getOs(context) + " uninstall -g @angular/cli");

    assertThat(context).logAtInfo().hasMessage("Successfully uninstalled ng");
  }

  /**
   * Tests if {@link Ng} run works correctly across all three operating systems.
   *
   * @param os Operating system
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("ng" + getNgBinaryName(context) + " " + getOs(context) + " --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm" + getNpmBinaryName(context) + " " + getOs(context) + " install -g @angular/cli@18.0.1");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }

  private String getOs(IdeTestContext context) {
    String os = "";
    if (context.getSystemInfo().isWindows()) {
      os = "windows";
    }
    if (context.getSystemInfo().isLinux()) {
      os = "linux";
    }
    if (context.getSystemInfo().isMac()) {
      os = "mac";
    }
    return os;
  }

  private String getNpmBinaryName(IdeTestContext context) {
    String os = "";
    if (context.getSystemInfo().isWindows()) {
      os = "cmdbin";
    }
    return os;
  }

  private String getNgBinaryName(IdeTestContext context) {
    String os = "";
    if (context.getSystemInfo().isWindows()) {
      os = "cmd";
    }
    return os;
  }
}
