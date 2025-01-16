package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.dotnet.DotNet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.npm.Npm;

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
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully uninstalled " + npm),
        IdeLogEntry.ofWarning("Couldn't uninstall " + dotnet + " because we could not find an installation"));
    assertThat(context.getSoftwarePath().resolve(npm)).doesNotExist();
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
    assertThat(context).logAtWarning().hasMessage("Couldn't uninstall " + eclipse + " because we could not find an installation");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(eclipse)));
  }

  @Test
  public void testUninstallCommandletRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    CommandletManager commandletManager = getCommandletManager(context);
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Npm npmCommandlet = commandletManager.getCommandlet(Npm.class);
    uninstallCommandlet.tools.addValue(npmCommandlet);

    // act
    uninstallCommandlet.run();
    //assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully uninstalled npm"));
  }

  private CommandletManager getCommandletManager(IdeTestContext context) {

    return context.getCommandletManager();
  }
}
