package com.devonfw.tools.ide.tool.ng;

import org.junit.jupiter.api.Disabled;
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
   * Tests if {@link Ng} can be run properly.
   *
   * @param os Operating System.
   */
  @Disabled
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testNgRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NG);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Ng commandlet = new Ng(context);

    // act
    commandlet.run();

    // assert
    if (context.getSystemInfo().isWindows()) {
      assertThat(context).logAtInfo().hasMessage("ngcmd ");
    } else {
      assertThat(context).logAtInfo().hasMessage("ng ");
    }
  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("node/ng")).exists();
      assertThat(context.getSoftwarePath().resolve("node/ng.cmd")).exists();
    }
    assertThat(context).logAtSuccess().hasMessage("Successfully installed ng in version 18.0.1");
  }
}
