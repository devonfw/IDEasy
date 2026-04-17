package com.devonfw.tools.ide.tool.vscode;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

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
    context.getTestStartContext().getEntries().clear();
    // act
    commandlet.run();
    // assert
    assertThat(context).logAtDebug().hasNoMessage("Successfully installed plugin: ActivePlugin");
  }

  @Test
  void testInstallPluginUsesExtensionVersionIfConfigured() {

    IdeTestContext context = newContext(PROJECT_VSCODE);
    CapturingVscode vscodeCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("publisher.extension", "mockedPlugin", null, "1.2.3", true, null);
    Step step = context.newStep("Install plugin mockedPlugin");

    step.run(() -> vscodeCommandlet.installPlugin(plugin, step, new ProcessContextTestImpl(context)));

    assertThat(vscodeCommandlet.lastArgs).contains("--install-extension", "publisher.extension@1.2.3");
  }

  @Test
  void testInstallPluginUsesExtensionIdWithoutVersionByDefault() {

    IdeTestContext context = newContext(PROJECT_VSCODE);
    CapturingVscode vscodeCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("publisher.extension", "mockedPlugin", null, null, true, null);
    Step step = context.newStep("Install plugin mockedPlugin");

    step.run(() -> vscodeCommandlet.installPlugin(plugin, step, new ProcessContextTestImpl(context)));

    assertThat(vscodeCommandlet.lastArgs).contains("--install-extension", "publisher.extension");
    assertThat(vscodeCommandlet.lastArgs).doesNotContain("publisher.extension@null");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("vscode/bin/code.cmd")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("vscode/bin/code")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("vscode/.ide.software.version")).exists().hasContent("1.92.1");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed vscode in version 1.92.1");
  }

  /**
   * Test double for {@link Vscode} that captures CLI arguments passed to {@link #runTool(ProcessContext, ProcessMode, List)}
   * so tests can assert command construction without spawning an external process.
   */
  private static class CapturingVscode extends Vscode {

    private List<String> lastArgs;

    private CapturingVscode(IdeTestContext context) {

      super(context);
      this.lastArgs = List.of();
    }

    @Override
    public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

      // Capture effective CLI args for assertions in unit tests.
      this.lastArgs = new ArrayList<>(args);
      // Return a successful dummy result to keep tests isolated from real VS Code execution.
      return new ProcessResultImpl("code", "code", 0, List.of());
    }
  }
}
