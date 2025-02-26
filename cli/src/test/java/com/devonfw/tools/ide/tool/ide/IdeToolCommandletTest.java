package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
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

  @Test
  public void testCheckEditionConflictInstallation() {
    // arrange
    IdeContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();
    // assert

    assertThat(commandlet.retrieveEditionMarkerFilePath(commandlet.getName())).exists();
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    commandlet.setEdition("ultimate");
    commandlet.run();

    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
  }
}
