package com.devonfw.tools.ide.tool.npm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Integration test of {@link Npm}.
 */
public class NpmTest extends AbstractIdeContextTest {

  private static final String PROJECT_NPM = "npm";

  /**
   * Tests if the {@link Npm} install works correctly across all three operating systems.
   *
   * @param os Operating system
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNpmInstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM);
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
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNpmRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Npm commandlet = new Npm(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("npm" + getBinaryType(context) + " " + getOs(context) + " --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("npm/.ide.software.version")).exists().hasContent("9.9.2");
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

  private String getBinaryType(IdeTestContext context) {
    String os = "";
    if (context.getSystemInfo().isWindows()) {
      os = "cmd";
    }
    return os;
  }
}
