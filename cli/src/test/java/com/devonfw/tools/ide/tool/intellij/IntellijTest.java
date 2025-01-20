package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.commandlet.InstallPluginCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Intellij}.
 */
@WireMockTest
public class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstall(String os) throws IOException {

    // arrange
    setupMockedPlugin(true);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    //if tool already installed
    commandlet.install();
    assertThat(this.context).logAtDebug().hasMessageContaining("Version 2023.3.3 of tool intellij is already installed");
  }

  /**
   * Tests if the {@link Intellij} can be installed properly, and a plugin can be installed separately afterward.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstallPluginAfterwards(String os) throws IOException {

    // arrange
    setupMockedPlugin(false);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();
    commandlet.installPlugin(commandlet.getPlugins().getById("mockedPlugin"), this.context.newStep("Install plugin MockedPlugin"));

    // assert
    checkInstallation(this.context);
  }

  /**
   * Tests if the {@link Intellij} can be installed properly, and a plugin can be uninstalled afterward.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijUninstallPluginAfterwards(String os) throws IOException {

    // arrange
    setupMockedPlugin(true);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    // act
    commandlet.uninstallPlugin(commandlet.getPlugins().getById("mockedPlugin"));

    //assert
    assertThat(context.getPluginsPath().resolve("intellij").resolve("mockedPlugin").resolve("MockedClass.class")).doesNotExist();
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijRun(String os) throws IOException {

    // arrange
    setupMockedPlugin(true);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);
    this.context.info("Starting testIntellijRun on {}", os);

    // act
    commandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("intellijtest")).hasContent(
        "intellij " + this.context.getSystemInfo().getOs() + " " + this.context.getWorkspacePath());
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can install plugins with custom url.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijPluginInstallWithCustomRepoUrl(String os) throws IOException {

    // arrange
    setupMockedPluginWithCustomToolUrl();
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("customRepoTest")).hasContent(
        "custom plugin repo url is: http://customRepo");
  }

  /**
   * Tests if {@link InstallPluginCommandlet} can install intellij plugins with custom url.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstallPluginWithCustomRepoUrl(String os) throws IOException {

    // arrange
    setupMockedPluginWithCustomToolUrl();
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    InstallPluginCommandlet commandlet = context.getCommandletManager().getCommandlet(InstallPluginCommandlet.class);
    commandlet.tool.setValueAsString("intellij", context);
    commandlet.plugin.setValueAsString("MockedPlugin", context);
    // act
    commandlet.run();

    // assert
    assertThat(context).logAtSuccess().hasEntries("Successfully ended step 'Install plugin: MockedPlugin'.");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed intellij in version 2023.3.3");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin MockedPlugin'.");
  }

  private void setupMockedPlugin(boolean mockedPluginActive) throws IOException {

    String content = "plugin_id=mockedPlugin\nplugin_active=" + mockedPluginActive;
    Files.write(this.context.getSettingsPath().resolve("intellij").resolve("plugins").resolve("MockedPlugin.properties"),
        content.getBytes(StandardCharsets.UTF_8));
  }

  private void setupMockedPluginWithCustomToolUrl() throws IOException {

    String content = "plugin_id=mockedPlugin\nplugin_active=true\nplugin_url=http://customRepo";
    Files.write(this.context.getSettingsPath().resolve("intellij").resolve("plugins").resolve("MockedPlugin.properties"),
        content.getBytes(StandardCharsets.UTF_8));
  }

}
