package com.devonfw.tools.ide.commandlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Integration test of {@link UninstallCommandlet}.
 */
public class UninstallCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link UninstallCommandlet} run.
   *
   */
  @Test
  public void testUninstallCommandletRun_WithExistingCommandlet() {

    // arrange
    String toolName = "npm";
    IdeTestContext context = newContext(PROJECT_BASIC);
    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.tool.setValueAsString(toolName, context);
    // act
    uninstallCommandlet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully uninstalled " + toolName);
    assertThat(Files.notExists(context.getSoftwarePath().resolve(toolName)));
  }

  @Test
  public void testUninstallCommandletRun_WithNonExistingCommandlet() {

    // arrange
    String toolName = "eclipse";
    IdeTestContext context = newContext(PROJECT_BASIC);
    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.tool.setValueAsString(toolName, context);
    // act
    uninstallCommandlet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "An installed version of " + toolName + " does not exist");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(toolName)));
  }

  @Test
  public void testUninstallCommandletRun_ThrowsException() {

    // arrange
    String toolName = "npm";
    IdeTestContext context = newContext(PROJECT_BASIC);

    FileAccess mockFileAccess = mock(FileAccess.class);
    doThrow(new IllegalStateException()).when(mockFileAccess).delete(any(Path.class));
    context.setMockFileAccess(mockFileAccess);

    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.tool.setValueAsString(toolName, context);
    // act
    try {
      uninstallCommandlet.run();
    } catch (IllegalStateException e) {
      // assert
      assertThat(e).hasMessageContaining("Couldn't uninstall " + toolName);
    }
  }
}
