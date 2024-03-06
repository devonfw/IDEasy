package com.devonfw.tools.ide.tool.npm;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

public class NpmTest extends AbstractIdeContextTest {

  private static final String PROJECT_NPM = "NPM";

  @Test
  public void testNpmInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM);

    Npm commandlet = new Npm(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("node/npm")).exists().hasContent("#!/bin/bash\n" + "echo \"npmbin $*\"");
      assertThat(context.getSoftwarePath().resolve("node/npm.cmd")).exists().hasContent("@echo off\n" + "echo npmcmdbin %*");
    }
    assertThat(context.getSoftwarePath().resolve("npm/.ide.software.version")).exists().hasContent("9.9.2");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed npm in version 9.9.2");
  }
}