package com.devonfw.tools.ide.tool.intellij;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Intellij}.
 */
@WireMockTest
class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testIntellijInstall(String os) {

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
  void testIntellijUninstallPluginAfterwards(String os) {

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
  void testIntellijRun(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);
    System.out.println("Starting testIntellijRun on " + os);

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
  void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext("intellij");

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: ActivePlugin");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    // part 2 of test

    // arrange
    context.getLogger().getEntries().clear();
    // act
    commandlet.run();
    // assert
    assertThat(context).logAtDebug().hasNoMessage("Successfully installed plugin: ActivePlugin");
  }

  /**
   * Tests by using 2 installations of intellij with different editions, if the plugins get re-installed and if all marker files get re-initialized properly.
   */
  @Test
  void testCheckEditionConflictInstallation() {
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

  /**
   * Tests if the repository commandlet can trigger an import of a mvn and a gradle project.
   */
  @Test
  void testIntellijMvnAndGradleRepositoryImport() {
    // arrange
    IdeTestContext context = newContext("intellij");
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);

    // act
    rc.run();

    // assert
    assertThat(context.getWorkspacePath().resolve(".idea").resolve("misc.xml")).hasContent("""
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <project version="4">
          <component name="MavenProjectsManager">
            <option name="originalFiles">
              <list>
                <option value="$PROJECT_DIR$/test_mvn/pom.xml"/>
              </list>
            </option>
          </component>
        </project>
        """);
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve("test").resolve(".idea").resolve("gradle.xml")).hasContent("""
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <project version="4">
          <component migrationVersion="1" name="GradleMigrationSettings"/>
          <component name="GradleSettings">
            <option name="linkedExternalProjectsSettings">
              <GradleProjectSettings>
                <option name="externalProjectPath" value="$PROJECT_DIR$/subfolder/test_gradle"/>
              </GradleProjectSettings>
            </option>
          </component>
        </project>
        """);
  }


  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context.getWorkspacePath().resolve("idea.properties")).exists();
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7", true),
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3", true));
    assertThat(context).logAtDebug().hasMessage("Omitting installation of inactive plugin InactivePlugin (inactivePlugin).");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin ActivePlugin'.");
  }

}
