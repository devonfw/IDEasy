package com.devonfw.tools.ide.tool.vscode;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Vscode} class.
 */
class VscodeTest extends AbstractIdeContextTest {

  private static final String PROJECT_VSCODE = "vscode";

  @Test
  void testVscodeInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // install
    vscodeCommandlet.install();

    // assert
    checkInstallation(context);
  }

  @Test
  void testVscodeRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // install
    vscodeCommandlet.run();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if after the installation of vscode the expected plugin marker file is existing.
   */
  @Test
  void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);

    // act
    Vscode commandlet = context.getCommandletManager().getCommandlet(Vscode.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: mockedPlugin");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("mockedPlugin"))).exists();
    assertThat(context.getIdeHome().resolve("plugins").resolve("vscode")).exists();

    // part 2 of test

    // arrange
    context.getLogger().getEntries().clear();
    // act
    commandlet.run();
    // assert
    assertThat(context).logAtDebug().hasNoMessage("Successfully installed plugin: ActivePlugin");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("vscode/bin/code.cmd")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("vscode/bin/code")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("vscode/.ide.software.version")).exists().hasContent("1.92.1");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed vscode in version 1.92.1");
  }
}
