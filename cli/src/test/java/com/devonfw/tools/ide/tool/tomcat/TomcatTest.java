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

  @ParameterizedTest
  @ValueSource(strings = { "mac", "windows", "linux" })
  public void testTomcatRun(String os) {

    IdeTestContext context = newContext(PROJECT_TOMCAT);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Tomcat tomcatCommandlet = new Tomcat(context);
    tomcatCommandlet.command.setValue(TomcatCommand.START);

    // act
    tomcatCommandlet.run();

    // assert
    checkRunningTomcat(context);
  }

  private void checkDependencyInstallation(IdeTestContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "The version 17.0.10_7 of the dependency java is being installed");
    assertLogMessage(context, IdeLogLevel.INFO, "The version 17.0.10_7 of the dependency java was successfully installed");

  }

  private void checkRunningTomcat(IdeTestContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "Tomcat is running at localhost on the following port (default 8080):");
    assertLogMessage(context, IdeLogLevel.INFO, "49152");

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