package com.devonfw.tools.ide.tool.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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

  @Test
  void testGetPluginsMap() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);

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
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getStartContext().setForcePlugins(true);
    final ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor plugin = ToolPluginDescriptor.of(context.getSettingsPath().resolve(ANY_EDIT_PLUGIN_PATH), context, false);
    pluginBasedCommandlet.createPluginMarkerFile(plugin);

    //act
    pluginBasedCommandlet.installPlugins(List.of(plugin), new ProcessContextTestImpl(context));

    //assert - Check if we skip the markerfile-check because we force the plugins to install
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin anyedit (1/1)'.");
    assertThat(context).log().hasNoMessageContaining("Skipping installation of plugin 'anyedit'");
  }

  @Test
  void testInstallPluginsProgressSkipsInstalledPlugins() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor anyedit = new ToolPluginDescriptor("anyedit", "anyedit", null, null, true, Set.of(), Set.of());
    ToolPluginDescriptor quickrex = new ToolPluginDescriptor("quickrex", "quickrex", null, null, true, Set.of(), Set.of());
    ToolPluginDescriptor startexplorer = new ToolPluginDescriptor("startexplorer", "startexplorer", null, null, true, Set.of(), Set.of());
    commandlet.createPluginMarkerFile(anyedit);
    commandlet.createPluginMarkerFile(startexplorer);

    commandlet.installPlugins(List.of(anyedit, quickrex, startexplorer), new ProcessContextTestImpl(context));

    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin quickrex (1/1)'.");
    assertThat(context).log().hasNoMessageContaining("Install plugin anyedit");
    assertThat(context).log().hasNoMessageContaining("Install plugin startexplorer");
    assertThat(commandlet.retrievePluginMarkerFilePath(quickrex)).exists();

    context.getTestStartContext().getEntries().clear();
    commandlet.installPlugins(List.of(anyedit, quickrex, startexplorer), new ProcessContextTestImpl(context));

    assertThat(context).log().hasNoMessageContaining("Install plugin ");
  }

  @Test
  void testInstallPluginsProgressExcludesInactiveAndExcludedPlugins() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor installed = new ToolPluginDescriptor("installed", "installed", null, null, true, Set.of(), Set.of());
    ToolPluginDescriptor first = new ToolPluginDescriptor("first", "first", null, null, true, Set.of(), Set.of());
    ToolPluginDescriptor inactive = new ToolPluginDescriptor("inactive", "inactive", null, null, false, Set.of(), Set.of());
    ToolPluginDescriptor excluded = new ToolPluginDescriptor("excluded", "excluded", null, null, true, Set.of(), Set.of(commandlet.getConfiguredEdition()));
    ToolPluginDescriptor second = new ToolPluginDescriptor("second", "second", null, null, true, Set.of(), Set.of());
    commandlet.createPluginMarkerFile(installed);

    commandlet.installPlugins(List.of(installed, first, inactive, excluded, second), new ProcessContextTestImpl(context));

    assertThat(context).logAtSuccess().hasEntries("Successfully ended step 'Install plugin first (1/2)'.",
        "Successfully ended step 'Install plugin second (2/2)'.");
    assertThat(context).log().hasNoMessageContaining("Install plugin installed");
    assertThat(context).log().hasNoMessageContaining("Install plugin inactive");
    assertThat(context).log().hasNoMessageContaining("Install plugin excluded");
    assertThat(context).logAtDebug().hasMessageContaining("Omitting installation of inactive plugin inactive");
    assertThat(commandlet.retrievePluginMarkerFilePath(first)).exists();
    assertThat(commandlet.retrievePluginMarkerFilePath(second)).exists();
  }

  @Test
  void testExtraPluginsProgressSkipsInstalledPlugins() {

    IdeTestContext context = newContext(PROJECT_EXTRA_PLUGINS);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPlugins plugins = commandlet.getPlugins();
    commandlet.createPluginMarkerFile(plugins.getByName("spotbugs"));

    commandlet.installPlugins(plugins.getPlugins(), new ProcessContextTestImpl(context));

    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin anyedit (1/1)'.");
    assertThat(context).log().hasNoMessageContaining("Install plugin spotbugs");
    assertThat(context).log().hasNoMessageContaining("Install plugin checkstyle");
    assertThat(commandlet.retrievePluginMarkerFilePath(plugins.getByName("anyedit"))).exists();
  }

  @Test
  void testParseVersionAndLegacyVersion(@TempDir Path tempDir) throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
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

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("plugin-id", "plugin-name", null, "1.2.3+build/4", true, Set.of(), Set.of());

    Path markerFilePath = pluginBasedCommandlet.retrievePluginMarkerFilePath(plugin);

    assertThat(markerFilePath).isNotNull();
    assertThat(markerFilePath.getFileName().toString()).contains("plugin-name.version-1.2.3_build_4");
  }

  @Test
  void testCreatePluginMarkerFileDeletesOtherVersionMarkers() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);
    ToolPluginDescriptor versionA = new ToolPluginDescriptor("plugin-id", "plugin-name", null, "1.0.0", true, Set.of(), Set.of());
    ToolPluginDescriptor versionB = new ToolPluginDescriptor("plugin-id", "plugin-name", null, "2.0.0", true, Set.of(), Set.of());

    pluginBasedCommandlet.createPluginMarkerFile(versionA);
    Path markerAPath = pluginBasedCommandlet.retrievePluginMarkerFilePath(versionA);
    Path markerBPath = pluginBasedCommandlet.retrievePluginMarkerFilePath(versionB);
    assertThat(markerAPath).exists();

    pluginBasedCommandlet.createPluginMarkerFile(versionB);

    assertThat(markerBPath).exists();
    assertThat(markerAPath).doesNotExist();
  }

  @Test
  void testExtraPluginsAreInstalled() {

    IdeTestContext context = newContext(PROJECT_EXTRA_PLUGINS, null, true);
    context.getStartContext().setForcePlugins(true);
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    pluginBasedCommandlet.installPlugins(pluginBasedCommandlet.getPlugins().getPlugins(), new ProcessContextTestImpl(context));

    // anyedit is configured as inactive but listed in ECLIPSE_EXTRA_PLUGINS - has to be installed
    assertThat(context).logAtSuccess().hasMessageContaining("Install plugin anyedit (");
    // spotbugs is configured as active and not listed - has to be installed as before
    assertThat(context).logAtSuccess().hasMessageContaining("Install plugin spotbugs (");
    // checkstyle is configured as inactive and not listed - has to stay omitted
    assertThat(context).log().hasMessageContaining("Omitting installation of inactive plugin checkstyle");
    assertThat(context).log().hasNoMessageContaining("Install plugin checkstyle");
  }

  @Test
  void testExtraPluginsDoNotModifyConfiguredState() {

    IdeTestContext context = newContext(PROJECT_EXTRA_PLUGINS, null, false);
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolPlugins toolPlugins = pluginBasedCommandlet.getPlugins();

    // the loaded configuration must stay untouched - activation happens only while installing
    assertThat(toolPlugins.getByName("anyedit").active()).isFalse();
    assertThat(toolPlugins.getByName("checkstyle").active()).isFalse();
    assertThat(toolPlugins.getByName("spotbugs").active()).isTrue();
  }

  @Test
  void testUndefinedExtraPluginIsIgnored() {

    IdeTestContext context = newContext(PROJECT_EXTRA_PLUGINS, null, false);
    ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    pluginBasedCommandlet.getExtraPlugins(pluginBasedCommandlet.getPlugins().getPlugins());

    assertThat(context).logAtInfo().hasMessageContaining("doesnotexist");
  }
}
