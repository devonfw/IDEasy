package com.devonfw.tools.ide.tool.pip;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ide.IdeFeatures;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.tool.python.Python;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link PipBasedIdeToolCommandlet}
 *
 **/
@WireMockTest
class PipBasedIdeToolCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /** The (dummy) name of the tool managed by the test commandlet. */
  private static final String TOOL = "mockedide";

  /** Concrete {@link PipBasedIdeToolCommandlet} used as test double. */
  private static class TestPipPluginCommandlet extends PipBasedIdeToolCommandlet {

    /**
     * The constructor.
     *
     * @param context the {@link IdeTestContext}.
     */
    TestPipPluginCommandlet(IdeTestContext context) {

      super(context, TOOL, Set.of(Tag.PYTHON));
    }
  }

  /**
   * Tests that the plugin installation path of a pip-based IDE points at the shared python environment (the parent tool) that holds the IDE itself.
   */
  @Test
  void testPluginsInstallationPathIsSharedPythonEnvironment() {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP);
    TestPipPluginCommandlet commandlet = new TestPipPluginCommandlet(context);
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // assert
    assertThat(commandlet.getPluginsInstallationPath()).isEqualTo(python.getToolPath());
    assertThat(commandlet.getPluginsInstallationPath()).isEqualTo(commandlet.getToolPath());
  }

  /**
   * Tests that installing a plugin of a pip-based IDE is delegated to the package manager (pip): the plugin's python package id is used and the installation is
   * reported as successful.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port providing the mocked PyPI index.
   */
  @Test
  void testInstallPluginIsDelegatedToPackageManager(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    TestPipPluginCommandlet commandlet = new TestPipPluginCommandlet(context);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("testplugin", "TestPlugin", null, "1.0.0", true, null, null);
    Step step = context.newStep("Install plugin TestPlugin");

    // act
    step.run(() -> commandlet.installPlugin(plugin, step, new ProcessContextTestImpl(context)));

    // assert
    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: TestPlugin");
  }

  /**
   * Tests that a pip-based IDE supports IDE workspace configuration via {@link IdeFeatures} and that {@link IdeFeatures#getIdeMetadataPath()} resolves to
   * {@code $IDE_HOME/.ide/«tool»/«workspace»}.
   */
  @Test
  void testGetIdeMetadataPath() {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP);
    TestPipPluginCommandlet commandlet = new TestPipPluginCommandlet(context);

    // act
    Path metadataPath = commandlet.getIdeMetadataPath();

    // assert
    assertThat(commandlet).isInstanceOf(IdeFeatures.class);
    assertThat(metadataPath).startsWithRaw(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE));
    assertThat(metadataPath).endsWithRaw(Path.of(TOOL, context.getWorkspaceName()));
    assertThat(metadataPath).isNotEqualTo(context.getWorkspacePath());
  }

  /**
   * Tests that {@link PipBasedIdeToolCommandlet#runTool(List)} (the entry point of {@code ide run «tool»}) triggers
   * {@link PipBasedIdeToolCommandlet#configureWorkspace()}, i.e. the workspace configuration is part of the run flow for a pip-installed IDE.
   */
  @Test
  void testRunToolTriggersWorkspaceConfiguration() {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP);
    AtomicBoolean workspaceConfigured = new AtomicBoolean(false);
    TestPipPluginCommandlet runToolCommandlet = new TestPipPluginCommandlet(context) {
      @Override
      public void configureWorkspace() {
        workspaceConfigured.set(true);
      }

      @Override
      public ProcessResult runTool(ToolInstallRequest request, ProcessMode processMode, List<String> args) {
        // short-circuit the real installation and process launch
        return new ProcessResultImpl(getName(), getName(), ProcessResult.SUCCESS, List.of());
      }
    };

    // act
    runToolCommandlet.runTool(List.of());

    // assert - runTool triggered the workspace configuration as part of the run flow
    assertThat(workspaceConfigured).isTrue();
  }

}
