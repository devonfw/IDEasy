package com.devonfw.tools.ide.tool.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link PluginBasedCommandlet}.
 */
public class PluginBasedCommandletTest extends AbstractIdeContextTest {

  @Test
  void testGetPluginsMap() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    String tool = "eclipse";
    Set<Tag> tags = null;
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, tool, tags);

    ToolPlugins pluginsMap = pluginBasedCommandlet.getPlugins();
    assertThat(pluginsMap).isNotNull();

    assertThat(pluginsMap.getByName("checkstyle")).isNotNull();
    assertThat(pluginsMap.getByName("anyedit")).isNotNull();

    ToolPluginDescriptor plugin1 = pluginsMap.getByName("checkstyle");
    assertNotNull(plugin1);
    assertThat(plugin1.name()).isEqualTo("checkstyle");

    ToolPluginDescriptor plugin2 = pluginsMap.getByName("anyedit");
    assertNotNull(plugin2);
    assertThat(plugin2.name()).isEqualTo("anyedit");

    // Check if anyedit plugin has value "false" --> value from user directory
    assertThat(plugin2.active()).isFalse();
  }
}
