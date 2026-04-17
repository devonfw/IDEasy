package com.devonfw.tools.ide.tool.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;

/**
 * Test of {@link PluginBasedCommandlet}.
 */
class PluginBasedCommandletTest extends AbstractIdeContextTest {

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
    assertThat(plugin1).isNotNull();
    assertThat(plugin1.name()).isEqualTo("checkstyle");
    assertThat(plugin1.version()).isNull();

    ToolPluginDescriptor plugin2 = pluginsMap.getByName("anyedit");
    assertThat(plugin2).isNotNull();
    assertThat(plugin2.name()).isEqualTo("anyedit");
    assertThat(plugin2.version()).isNull();

    // Check if anyedit plugin has value "false" --> value from user directory
    assertThat(plugin2.active()).isFalse();
  }

  @Test
  void testInstallPluginsWithForce() {

    //arrange
    context.getStartContext().setForcePlugins(true);
    final ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    //act
    pluginBasedCommandlet.installPlugins(
        List.of(ToolPluginDescriptor.of(context.getSettingsPath().resolve(ANY_EDIT_PLUGIN_PATH), context, false)),
        new ProcessContextTestImpl(context));

    //assert - Check if we skip the markerfile-check because we force the plugins to install
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin anyedit'.");
    assertThat(context).log().hasNoMessageContaining("Skipping installation of plugin '{}' due to existing marker file: ");
  }

  @Test
  void testParseVersionAndLegacyVersion(@TempDir Path tempDir) throws IOException {

    Path versionProperties = tempDir.resolve("version.properties");
    Files.writeString(versionProperties, "id=plugin-id\nactive=true\nversion=1.2.3\n");
    ToolPluginDescriptor plugin = ToolPluginDescriptor.of(versionProperties, context, false);
    assertThat(plugin.version()).isEqualTo("1.2.3");

    Path legacyVersionProperties = tempDir.resolve("legacy-version.properties");
    Files.writeString(legacyVersionProperties, "plugin_id=plugin-id\nplugin_active=true\nplugin_version=2.0.0\n");
    ToolPluginDescriptor legacyPlugin = ToolPluginDescriptor.of(legacyVersionProperties, context, false);
    assertThat(legacyPlugin.version()).isEqualTo("2.0.0");
  }

  @Test
  void testMarkerFileContainsVersionSegment() {

    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("plugin-id", "plugin-name", null, "1.2.3+build/4", true, Set.of());

    Path markerFilePath = pluginBasedCommandlet.retrievePluginMarkerFilePath(plugin);

    assertThat(markerFilePath).isNotNull();
    assertThat(markerFilePath.getFileName().toString()).contains("plugin-name.version-1.2.3_build_4");
  }
}
