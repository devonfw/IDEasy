package com.devonfw.tools.ide.tool.vscode;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Vscode} class.
 */
public class VscodeTest extends AbstractIdeContextTest {

  private static final String PROJECT_VSCODE = "vscode";


  @Test
  public void testVscodeInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // install
    vscodeCommandlet.install();

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("vscode/bin/code.cmd")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("vscode/bin/code")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("vscode/.ide.software.version")).exists().hasContent("1.92.1");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed vscode in version 1.92.1");

    //check plugins folder
    assertThat(context.getIdeHome().resolve("plugins").resolve("vscode")).exists();

    //check Recommendations
    assertThat(context.getWorkspacePath().resolve(".vscode").resolve("extensions.json")).exists()
        .hasContent("{\"recommendations\":[\"esbenp.prettier-vscode\",\"mockedPlugin2\"]}");
  }
}
