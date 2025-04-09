package com.devonfw.tools.ide.tool.pycharm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Pycharm}.
 */
@WireMockTest
public class PycharmTest extends AbstractIdeContextTest {

  private static final String PROJECT_PYCHARM = "pycharm";
  private final IdeTestContext context = newContext(PROJECT_PYCHARM);

  /**
   * Tests if the {@link Pycharm} can be installed properly.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testPycharmInstall(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Pycharm commandlet = new Pycharm(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    //if tool already installed
    commandlet.install();
    assertThat(this.context).logAtDebug().hasMessageContaining("Version 2023.3.3 of tool pycharm is already installed");
  }

  /**
   * Tests if the {@link Pycharm} can be installed properly, and a plugin can be uninstalled afterward.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testPycharmUninstallPluginAfterwards(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Pycharm commandlet = new Pycharm(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    // act
    commandlet.uninstallPlugin(commandlet.getPlugins().getById("activePlugin"));

    //assert
    assertThat(context.getPluginsPath().resolve("pycharm").resolve("mockedPlugin").resolve("MockedClass.class")).doesNotExist();
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testPycharmRun(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Pycharm commandlet = new Pycharm(this.context);
    this.context.info("Starting testPycharmRun on {}", os);

    // act
    commandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("pycharmtest")).hasContent(
        "pycharm " + this.context.getSystemInfo().getOs() + " " + this.context.getWorkspacePath());
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can install plugins with custom url.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testPycharmPluginInstallWithCustomRepoUrl(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Pycharm commandlet = new Pycharm(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("customRepoTest")).hasContent(
        "custom plugin repo url is: http://customRepo");
  }

  /**
   * Tests if after the installation of pycharm the expected plugin marker file is existing.
   */
  @Test
  public void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext("pycharm");

    // act
    Pycharm commandlet = context.getCommandletManager().getCommandlet(Pycharm.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: ActivePlugin");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    commandlet.run();
    assertThat(context).logAtDebug().hasMessage("Markerfile for IDE: pycharm and active plugin: ActivePlugin already exists.");
  }

  /**
   * Tests by using 2 installations of pycharm with different editions, if the plugins get re-installed and if all marker files get re-initialized properly.
   */
  @Test
  public void testCheckEditionConflictInstallation() {
    // arrange
    IdeTestContext context = newContext("pycharm");
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);

    // act
    Pycharm commandlet = context.getCommandletManager().getCommandlet(Pycharm.class);
    commandlet.run();

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    // act
    commandlet.setEdition("professional");
    commandlet.run();

    // assert
    assertThat(context).logAtDebug()
        .hasEntries("Plugin marker file " + context.getIdeHome().resolve(".ide").resolve("plugin.pycharm.professional.ActivePlugin") + " got deleted.");
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
  }


  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("pycharm/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed pycharm in version 2023.3.3");
    assertThat(context).logAtDebug().hasEntries("Omitting installation of inactive plugin InactivePlugin (inactivePlugin).");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin ActivePlugin'.");
  }

}
