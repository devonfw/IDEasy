package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    Map<String, PluginDescriptor> pluginsMap = pluginBasedCommandlet.getPluginsMap();
    assertThat(pluginsMap).isNotNull();

    assertThat(pluginsMap.containsKey("checkstyle")).isTrue();
    assertThat(pluginsMap.containsKey("anyedit")).isTrue();

    PluginDescriptor plugin1 = pluginsMap.get("checkstyle");
    assertNotNull(plugin1);
    assertThat(plugin1.getName()).isEqualTo("checkstyle");

    PluginDescriptor plugin2 = pluginsMap.get("anyedit");
    assertNotNull(plugin2);
    assertThat(plugin2.getName()).isEqualTo("anyedit");

    // Check if anyedit plugin has value "false" --> value from user directory
    assertThat(plugin2.isActive()).isFalse();
  }
}
