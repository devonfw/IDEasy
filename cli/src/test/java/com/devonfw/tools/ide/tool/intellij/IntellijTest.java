package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

/**
 * Integration test of {@link Intellij}.
 */
public class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";

  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstall(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);

    //if tool already installed
    commandlet.install();
    assertLogMessage(context, IdeLogLevel.DEBUG, "Version 2023.3.3 of tool intellij is already installed");
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijRun(String os) {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(context);

    // act
    commandlet.run();

    // assert
    SystemInfo currentSystemInfo = context.getSystemInfo();
    Path workspacePath = this.context.getWorkspacePath();
    
    if (currentSystemInfo.isMac()) {
      assertLogMessage(context, IdeLogLevel.INFO, "intellij mac " + workspacePath);
    } else if (currentSystemInfo.isLinux()) {
      assertLogMessage(context, IdeLogLevel.INFO, "intellij linux " + workspacePath);
    } else if (currentSystemInfo.isWindows()) {
      assertLogMessage(context, IdeLogLevel.INFO, "intellij windows " + workspacePath);
    }
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3");
  }
}
