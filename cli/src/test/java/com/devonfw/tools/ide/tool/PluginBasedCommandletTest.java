package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link PluginBasedCommandlet}.
 */
public class PluginBasedCommandletTest extends AbstractIdeContextTest {

  public IdeTestContext context = newContext(PROJECT_BASIC);

  public ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, "eclipse",
      null);

  Map<String, PluginDescriptor> pluginsMapById = pluginBasedCommandlet.getPluginsMap().getById();
  Map<String, PluginDescriptor> pluginsMapByName = pluginBasedCommandlet.getPluginsMap().getByName();

  @Test
  void testGetPluginsMap() {
    assertThat(pluginsMapById).isNotNull();
    assertThat(pluginsMapByName).isNotNull();

    assertThat(pluginsMapById.containsKey("net.sf.eclipsecs.feature.group")).isTrue();
    assertThat(pluginsMapById.containsKey("AnyEditTools.feature.group")).isTrue();
    assertThat(pluginsMapByName.containsKey("anyedit")).isTrue();
    assertThat(pluginsMapByName.containsKey("checkstyle")).isTrue();

    PluginDescriptor plugin1 = pluginsMapById.get("net.sf.eclipsecs.feature.group");
    assertNotNull(plugin1);
    assertThat(plugin1.getName()).isEqualTo("checkstyle");

    PluginDescriptor plugin2 = pluginsMapById.get("AnyEditTools.feature.group");
    assertNotNull(plugin2);
    assertThat(plugin2.getName()).isEqualTo("anyedit");

    // Check if anyedit plugin has value "false" --> value from user directory
    assertThat(plugin2.isActive()).isFalse();

    assertThat(pluginsMapById.containsKey("anyedit2")).isFalse();

    assertThat(pluginsMapById.size()).isEqualTo(2);
    assertThat(pluginsMapByName.size()).isEqualTo(2);
  }

  @Test
  void testGetPlugin() {
    // Testing with existing plugin ID
    PluginDescriptor pluginDescriptorById = pluginBasedCommandlet.getPlugin("net.sf.eclipsecs.feature.group");
    assertThat(pluginDescriptorById).isNotNull();
    assertThat(pluginDescriptorById.getName()).isEqualTo("checkstyle");
    assertThat(pluginDescriptorById.isActive()).isTrue();

    // Testing with existing plugin name
    PluginDescriptor pluginDescriptorByName = pluginBasedCommandlet.getPlugin("AnyEditTools.feature.group");
    assertThat(pluginDescriptorByName).isNotNull();
    assertThat(pluginDescriptorByName.getName()).isEqualTo("anyedit");
    assertThat(pluginDescriptorByName.isActive()).isFalse();

    // Testing with non-existing key
    assertThatThrownBy(() -> pluginBasedCommandlet.getPlugin("non-existing-key"))
        .isInstanceOf(CliException.class)
        .hasMessageContaining("Could not find plugin non-existing-key");
  }
}