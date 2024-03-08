package com.devonfw.tools.ide.tool.intellij;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.jmc.Jmc;

/**
 * Integration test of {@link Jmc}.
 */
public class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isLinux()) {

    }
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3");
  }
}
