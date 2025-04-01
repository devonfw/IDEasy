package com.devonfw.tools.ide.tool.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;

/**
 * Test of {@link PluginBasedCommandlet}.
 */
public class PluginBasedCommandletTest extends AbstractIdeContextTest {

  private final String ANY_EDIT_PLUGIN_PATH = "eclipse/plugins/anyedit.properties";
  private final String TOOL = "eclipse";

  private final Set<Tag> tags = null;
  private static IdeTestContext context;

  @BeforeAll
  static void setUp() {

    context = newContext(PROJECT_BASIC, null, false);
  }

  @Test
  void testGetPluginsMap() {

    final ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

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

  @Test
  void testInstallPluginsWithForce() {

    //arrange
    context.setForcePlugins(true);
    final ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    //act
    pluginBasedCommandlet.installPlugins(
        List.of(ToolPluginDescriptor.of(context.getSettingsPath().resolve(ANY_EDIT_PLUGIN_PATH), context, false)),
        new ProcessContextTestImpl(context));

    //assert - Check if we skip the markerfile-check because we force the plugins to install
    assertThat(context).log().hasNoMessage("Markerfile for IDE: eclipse and active plugin: anyedit already exists.");
  }
}
