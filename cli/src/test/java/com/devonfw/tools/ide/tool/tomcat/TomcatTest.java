package com.devonfw.tools.ide.tool.tomcat;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

public class TomcatTest extends AbstractIdeContextTest {

  private static final String PROJECT_TOMCAT = "tomcat";

  @Test
  public void testTomcat() {

    // arrange
    IdeTestContext context = newContext(PROJECT_TOMCAT);
    Tomcat tomcatCommandlet = new Tomcat(context);

    // install
    tomcatCommandlet.install();

    // run
    tomcatCommandlet.command.setValue(TomcatCommand.START);
    tomcatCommandlet.run();

    // assert
    checkInstallation(context);
    checkDependencyInstallation(context);
    checkRunningTomcat(context);
  }

  private void checkDependencyInstallation(IdeTestContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "The version 17.0.10_7 of the dependency java is being installed");
    assertLogMessage(context, IdeLogLevel.INFO, "The version 17.0.10_7 of the dependency java was successfully installed");

  }

  private void checkRunningTomcat(IdeTestContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "Tomcat is running at localhost on the following port (default 8080):");
    assertLogMessage(context, IdeLogLevel.INFO, "8080");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("tomcat/bin/startup.bat")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("tomcat/bin/startup.sh")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("tomcat/.ide.software.version")).exists().hasContent("10.1.14");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed tomcat in version 10.1.14");
  }

}
