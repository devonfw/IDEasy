package com.devonfw.tools.ide.tool.intellij;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijInstall(String os) {

    // arrange
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
   * Tests if the {@link Intellij} can be installed properly, and a plugin can be uninstalled afterward.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijUninstallPluginAfterwards(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("customRepoTest")).hasContent(
        "custom plugin repo url is: http://customRepo");

    // act
    commandlet.uninstallPlugin(commandlet.getPlugins().getById("activePlugin"));

    //assert
    assertThat(context.getPluginsPath().resolve("intellij").resolve("activePlugin")).doesNotExist();
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testIntellijRun(String os) {

    // arrange
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
   * Tests if after the installation of intellij the expected plugin marker file is existing.
   */
  @Test
  public void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext("intellij");

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: ActivePlugin");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    commandlet.run();
    assertThat(context).logAtDebug().hasMessage("Markerfile for IDE: intellij and active plugin: ActivePlugin already exists.");
  }

  /**
   * Tests by using 2 installations of intellij with different editions, if the plugins get re-installed and if all marker files get re-initialized properly.
   */
  @Test
  public void testCheckEditionConflictInstallation() {
    // arrange
    IdeTestContext context = newContext("intellij");
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    // act
    commandlet.setEdition("ultimate");
    commandlet.run();

    // assert
    assertThat(context).logAtDebug()
        .hasEntries("Plugin marker file " + context.getIdeHome().resolve(".ide").resolve("plugin.intellij.intellij.ActivePlugin") + " got deleted.");
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
  }


  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed intellij in version 2023.3.3");
    assertThat(context).logAtDebug().hasEntries("Omitting installation of inactive plugin InactivePlugin (inactivePlugin).");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin ActivePlugin'.");
  }

}
