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
    commandlet.install(null);

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("node/npm")).exists().hasContent("#!/bin/bash\n" + "echo \"npmbin $*\"");
      assertThat(context.getSoftwarePath().resolve("node/npm.cmd")).exists().hasContent("@echo off\n" + "echo npmcmdbin %*");
      assertThat(context.getSoftwarePath().resolve("node/npx")).exists().hasContent("#!/bin/bash\n" + "echo \"npxbin $*\"");
      assertThat(context.getSoftwarePath().resolve("node/npx.cmd")).exists().hasContent("@echo off\n" + "echo npxcmdbin %*");
    }

    assertThat(context.getSoftwarePath().resolve("npm/.ide.software.version")).exists().hasContent("9.9.2");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed npm in version 9.9.2");
  }
}
