package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Test of {@link IdeToolCommandlet}.
 */
public class IdeToolCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests if .editorconfig was copied to workspace-folder after running an ide-tool.
   */
  @Test
  public void testConfigureWorkspace() {
    // arrange
    IdeContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();
    // act
    context.getCommandletManager().getCommandlet(Intellij.class).run();
    // assert
    assertThat(workspace.resolve(".editorconfig")).exists();
    assertThat(workspace.resolve(".intellij/config/idea.key")).exists();
    assertThat(workspace.resolve("user.properties")).exists().content().contains("ijversion=2023.3.3");
  }

  @Test
  public void testCheckPluginInstallation() {
    // arrange
    IdeContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();
    // assert

    assertThat(commandlet.retrieveEditionMarkerFilePath(commandlet.getName())).exists();
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
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
    assertThat(commandlet.retrieveEditionMarkerFilePath(commandlet.getName())).exists();
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
    assertThat(context.getPluginsPath().resolve("intellij").resolve(".intellij")).exists();

    // act
    commandlet.setEdition("ultimate");
    commandlet.run();

    // assert
    assertThat(context.getPluginsPath().resolve("intellij").resolve(".intellij")).doesNotExist();
    assertThat(context).logAtDebug().hasEntries("Matching edition marker file for ultimate was not found, re-installing plugins");
    assertThat(context).logAtDebug()
        .hasEntries("Plugin marker file " + context.getIdeHome().resolve(".ide").resolve("plugin.intellij.intellij.ActivePlugin") + " got deleted.");
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
    assertThat(context.getPluginsPath().resolve("intellij").resolve(".ultimate")).exists();
  }
}
