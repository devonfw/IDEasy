package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.dotnet.DotNet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.npm.Npm;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test of {@link UninstallCommandlet}.
 */
public class UninstallCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link UninstallCommandlet} run.
   */
  @Test
  public void testUninstallCommandletRun_WithExistingCommandlet() {

    // arrange
    String npm = "npm";
    String dotnet = "dotnet";
    IdeTestContext context = newContext(PROJECT_BASIC);
    CommandletManager commandletManager = getCommandletManager(context);
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Npm npmCommandlet = commandletManager.getCommandlet(Npm.class);
    DotNet dotnetCommandlet = commandletManager.getCommandlet(DotNet.class);

    ToolProperty tools = uninstallCommandlet.tools;
    tools.addValue(npmCommandlet);
    tools.addValue(dotnetCommandlet);

    // act
    uninstallCommandlet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully uninstalled " + npm);
    assertLogMessage(context, IdeLogLevel.WARNING, "An installed version of " + dotnet + " does not exist");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(npm)));
  }

  @Test
  public void testUninstallCommandletRun_WithNonExistingCommandlet() {

    // arrange
    String eclipse = "eclipse";
    IdeTestContext context = newContext(PROJECT_BASIC);
    CommandletManager commandletManager = getCommandletManager(context);
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Eclipse eclipseCommandlet = commandletManager.getCommandlet(Eclipse.class);
    uninstallCommandlet.tools.addValue(eclipseCommandlet);
    // act
    uninstallCommandlet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.WARNING, "An installed version of " + eclipse + " does not exist");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(eclipse)));
  }

  @Test
  public void testUninstallCommandletRun_ThrowsException() {

    // arrange
    FileAccessImpl mockFileAccess = mock(FileAccessImpl.class);
    IdeTestContext mockContext = mock(IdeTestContext.class);
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path softwarePath = context.getSoftwarePath();

    when(mockContext.getFileAccess()).thenReturn(mockFileAccess);

    when(mockContext.getCommandletManager()).thenReturn(new CommandletManagerImpl(mockContext));
    when(mockContext.getSoftwarePath()).thenReturn(softwarePath);

    doThrow(new IllegalStateException("Couldn't uninstall")).when(mockFileAccess).delete(any());
    CommandletManager commandletManager = getCommandletManager(mockContext);
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Npm npmCommandlet = commandletManager.getCommandlet(Npm.class);
    uninstallCommandlet.tools.addValue(npmCommandlet);

    // act
    uninstallCommandlet.run();

    //assert
    verify(mockContext).error("Couldn't uninstall npm");
  }

  private CommandletManager getCommandletManager(IdeTestContext context) {

    return context.getCommandletManager();
  }
}