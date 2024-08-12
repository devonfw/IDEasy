package com.devonfw.tools.ide.tool.tomcat;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessMode;

public class TomcatTest extends AbstractIdeContextTest {

  private static final String PROJECT_TOMCAT = "tomcat";

  @Test
  public void testTomcat() {

    // arrange
    IdeTestContext context = newContext(PROJECT_TOMCAT);
    Tomcat tomcatCommandlet = new Tomcat(context);

    // run
    tomcatCommandlet.runTool(ProcessMode.DEFAULT, null, true);

    // assert
    checkInstallation(context);
    checkDependencyInstallation(context);
    checkRunningTomcat(context);
  }

  private void checkDependencyInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasEntries("The version 17.0.10_7 of the dependency java is being installed",
        "The version 17.0.10_7 of the dependency java was successfully installed");
  }

  private void checkRunningTomcat(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessage("Tomcat is running at localhost on the following port (default 8080):");
    assertThat(context).logAtInfo().hasMessage("8080");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("tomcat/bin/catalina.bat")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("tomcat/bin/catalina.sh")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("tomcat/.ide.software.version")).exists().hasContent("10.1.14");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed tomcat in version 10.1.14");
  }

}
