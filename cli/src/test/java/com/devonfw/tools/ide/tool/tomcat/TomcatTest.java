package com.devonfw.tools.ide.tool.tomcat;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TomcatTest extends AbstractIdeContextTest {

  private static final String PROJECT_TOMCAT = "tomcat";

  @ParameterizedTest
  @ValueSource(strings = { "windows", "linux", "mac" })
  public void testTomcatInstall(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_TOMCAT);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Tomcat tomcatCommandlet = new Tomcat(context);

    // install
    tomcatCommandlet.install();

    // assert
    checkInstallation(context);
    checkDependencyInstallation(context);
  }

  private void checkDependencyInstallation(IdeTestContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "Necessary version of the dependency java is already installed in repository");

  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("tomcat/bin/tomcat")).exists().hasContent("#!/bin/bash\n" + "echo \"tomcat $*\"");
      assertThat(context.getSoftwarePath().resolve("tomcat/bin/tomcat.cmd")).exists().hasContent("@echo off\n" + "echo tomcat %*");
    } else if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("tomcat/tomcat")).exists();
    }
    assertThat(context.getSoftwarePath().resolve("tomcat/.ide.software.version")).exists().hasContent("10.1.14");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed tomcat in version 10.1.14");
  }

}