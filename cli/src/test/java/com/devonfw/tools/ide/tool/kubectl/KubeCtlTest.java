package com.devonfw.tools.ide.tool.kubectl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Integration test of {@link KubeCtl}.
 */
public class KubeCtlTest extends AbstractIdeContextTest {

  private static final String PROJECT_KUBECTL = "kubectl";

  /**
   * Tests the installation of {@link KubeCtl}.
   *
   * @param os the Operating system to run on
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testKubeCtlInstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_KUBECTL);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    KubeCtl commandlet = context.getCommandletManager().getCommandlet(KubeCtl.class);

    // act
    commandlet.install();

    // assert

  }
}
