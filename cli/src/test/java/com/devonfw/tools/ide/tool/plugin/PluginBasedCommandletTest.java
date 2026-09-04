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
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setForcePlugins(true);
    final ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    //act
    pluginBasedCommandlet.installPlugins(
        List.of(ToolPluginDescriptor.of(context.getSettingsPath().resolve(ANY_EDIT_PLUGIN_PATH), context, false)),
        new ProcessContextTestImpl(context));

    //assert - Check if we skip the markerfile-check because we force the plugins to install
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin anyedit (1/1)'.");
    assertThat(context).log().hasNoMessageContaining("Skipping installation of plugin '{}' due to existing marker file: ");
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

  @Test
  void testPluginPurgeRequiredOnFreshInstallation() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEditionAndVersion editionAndVersion = newEditionAndVersion(new ToolEdition(TOOL, TOOL), VersionIdentifier.of("1.90.0"));
    ToolInstallRequest request = newPurgeRequest(null, editionAndVersion);

    assertThat(commandlet.isPluginPurgeRequired(request)).isTrue();
  }

  @Test
  void testNoPluginPurgeOnUnchangedVersion() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEditionAndVersion sameEditionAndVersion = newEditionAndVersion(new ToolEdition(TOOL, TOOL), VersionIdentifier.of("1.90.0"));
    ToolInstallRequest request = newPurgeRequest(sameEditionAndVersion, sameEditionAndVersion);

    assertThat(commandlet.isPluginPurgeRequired(request)).isFalse();
  }

  @Test
  void testNoPluginPurgeOnFixVersionChange() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEdition edition = new ToolEdition(TOOL, TOOL);
    ToolEditionAndVersion editionAndVersion1 = newEditionAndVersion(edition, VersionIdentifier.of("1.90.0"));
    ToolEditionAndVersion editionAndVersion2 = newEditionAndVersion(edition, VersionIdentifier.of("1.90.1"));
    ToolInstallRequest request = newPurgeRequest(editionAndVersion1, editionAndVersion2);

    assertThat(commandlet.isPluginPurgeRequired(request)).isFalse();
  }

  @Test
  void testNoPluginPurgeOnMinorVersionChange() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEdition edition = new ToolEdition(TOOL, TOOL);
    ToolEditionAndVersion minorToolEditionAndVersion1 = newEditionAndVersion(edition, VersionIdentifier.of("1.90.0"));
    ToolEditionAndVersion minorToolEditionAndVersion2 = newEditionAndVersion(edition, VersionIdentifier.of("1.91.0"));
    ToolInstallRequest request = newPurgeRequest(minorToolEditionAndVersion1, minorToolEditionAndVersion2);

    assertThat(commandlet.isPluginPurgeRequired(request)).isFalse();
  }

  @Test
  void testPluginPurgeRequiredOnMajorVersionChange() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEdition edition = new ToolEdition(TOOL, TOOL);
    ToolEditionAndVersion majorToolEditionAndVersion1 = newEditionAndVersion(edition, VersionIdentifier.of("1.90.0"));
    ToolEditionAndVersion majorToolEditionAndVersion2 = newEditionAndVersion(edition, VersionIdentifier.of("2.0.0"));
    ToolInstallRequest request = newPurgeRequest(majorToolEditionAndVersion1, majorToolEditionAndVersion2);

    assertThat(commandlet.isPluginPurgeRequired(request)).isTrue();
  }

  @Test
  void testPluginPurgeRequiredOnEditionChange() {

    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    ExamplePluginBasedCommandlet commandlet = new ExamplePluginBasedCommandlet(context, TOOL, tags);

    ToolEditionAndVersion edition1AndVersion = newEditionAndVersion(new ToolEdition(TOOL, TOOL), VersionIdentifier.of("1.90.0"));
    ToolEditionAndVersion edition2AndVersion = newEditionAndVersion(new ToolEdition(TOOL, "cpp"), VersionIdentifier.of("1.90.0"));
    ToolInstallRequest request = newPurgeRequest(edition1AndVersion, edition2AndVersion);

    assertThat(commandlet.isPluginPurgeRequired(request)).isTrue();
  }

  private static ToolInstallRequest newPurgeRequest(ToolEditionAndVersion installed, ToolEditionAndVersion requested) {

    ToolInstallRequest request = new ToolInstallRequest(true);
    if (installed != null) {
      request.setInstalled(installed);
    }
    if (requested != null) {
      request.setRequested(requested);
    }
    return request;
  }

  private static ToolEditionAndVersion newEditionAndVersion(ToolEdition edition, VersionIdentifier version) {

    ToolEditionAndVersion result = new ToolEditionAndVersion(edition);
    result.setResolvedVersion(version);
    return result;
  }
}
