package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.version.GenericVersionRange;

/**
 * Test of {@link IdeToolCommandlet} using {@link IdeToolDummyCommandlet}.
 */
class IdeToolDummyCommandletTest extends AbstractIdeContextTest {

  /**
   * Run the dummy commandlet and test that only active plugins are passed to installPlugin method.
   *
   * @param tempDir the {@link TempDir}.
   */
  @Test
  void testDummyCommandlet(@TempDir Path tempDir) {

    AbstractIdeTestContext context = IdeTestContextMock.get();
    context.setPluginsPath(tempDir);
    context.setIdeHome(tempDir);
    context.setSettingsPath(Path.of("src/test/resources/settings/dummy"));
    IdeToolDummyCommandlet dummyCommandlet = new IdeToolDummyCommandlet(context, "dummy", Set.of(Tag.IDE));

    context.addCommandlet(dummyCommandlet);

    Commandlet dummy = context.getCommandletManager().getCommandlet("dummy");
    assertThat(dummy).isSameAs(dummyCommandlet);
    dummy.run();
    assertThat(dummyCommandlet.installedPlugins).hasSize(1);
    ToolPluginDescriptor plugin = dummyCommandlet.installedPlugins.getFirst();
    assertThat(plugin.id()).isEqualTo("plugin1-id");
    assertThat(plugin.url()).isEqualTo("https://dummy.com/plugins/plugin1-url");
  }

  /**
   * Dummy commandlet extending {@link IdeToolCommandlet} for testing.
   */
  public static class IdeToolDummyCommandlet extends IdeToolCommandlet {

    final List<ToolPluginDescriptor> installedPlugins;

    IdeToolDummyCommandlet(IdeContext context, String tool, Set<Tag> tags) {

      super(context, tool, tags);
      this.installedPlugins = new ArrayList<>();
    }

    @Override
    protected void configureWorkspace() {

      // disable workspace configuration since we have no IDE_HOME and therefore no settings
    }

    @Override
    public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, List<String> args) {

      // skip installation but trigger postInstall to test mocked plugin installation
      postInstall(new ToolInstallRequest(true));
      return new ProcessResultImpl(this.tool, this.tool, 0, List.of());
    }

    @Override
    public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

      this.installedPlugins.add(plugin);
      step.success("Dummy plugin " + plugin.name() + " installed.");
      return true;
    }

  }
}
