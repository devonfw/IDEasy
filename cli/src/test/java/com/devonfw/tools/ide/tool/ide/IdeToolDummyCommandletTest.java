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
import com.devonfw.tools.ide.context.IdeSlf4jContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.version.GenericVersionRange;

public class IdeToolDummyCommandletTest extends AbstractIdeContextTest {

  @Test
  public void testDummyCommandlet(@TempDir Path tempDir) {

    AbstractIdeTestContext context = new IdeSlf4jContext();
    context.setPluginsPath(tempDir);
    context.setSettingsPath(Path.of("src/test/resources/settings/dummy"));
    IdeToolDummyCommandlet dummyCommandlet = new IdeToolDummyCommandlet(context, "dummy", Set.of(Tag.IDE));

    context.addCommandlet(dummyCommandlet);

    Commandlet dummy = context.getCommandletManager().getCommandlet("dummy");
    assertThat(dummy).isSameAs(dummyCommandlet);
    dummy.run();
    assertThat(dummyCommandlet.installedPlugins).hasSize(1);
    ToolPluginDescriptor plugin = dummyCommandlet.installedPlugins.get(0);
    assertThat(plugin.id()).isEqualTo("plugin1-id");
    assertThat(plugin.url()).isEqualTo("https://dummy.com/plugins/plugin1-url");
  }

  public static class IdeToolDummyCommandlet extends IdeToolCommandlet {

    final List<ToolPluginDescriptor> installedPlugins;

    public IdeToolDummyCommandlet(IdeContext context, String tool, Set<Tag> tags) {

      super(context, tool, tags);
      this.installedPlugins = new ArrayList<>();
    }

    @Override
    protected void configureWorkspace() {

    }

    @Override
    public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, String... args) {

      postInstall(true);
      return new ProcessResultImpl(0, List.of(), List.of());
    }

    @Override
    public void installPlugin(ToolPluginDescriptor plugin, Step step) {

      this.installedPlugins.add(plugin);
      step.success("Dummy plugin " + plugin.name() + " installed.");
    }

  }
}
