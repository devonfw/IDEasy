package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.az.Azure;
import com.devonfw.tools.ide.tool.dotnet.DotNet;
import com.devonfw.tools.ide.tool.eclipse.Eclipse;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.node.Node;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Integration test of {@link UninstallCommandlet}.
 */
public class UninstallCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT = "edition-version-get-uninstall";

  /**
   * Mocks the installation of a tool, since forceUninstall depends on symlinks which are not distributed with git
   *
   * @param context the {@link IdeContext} to use.
   * @param tool the tool to mock installation of.
   */
  private static void mockInstallTool(IdeTestContext context, String tool) {

    Path pathToInstallationOfDummyTool = context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve(tool).resolve("testVersion");
    Path pathToLinkedSoftware = context.getSoftwarePath().resolve(tool);
    context.getFileAccess().symlink(pathToInstallationOfDummyTool, pathToLinkedSoftware);
  }

  /**
   * Test of {@link UninstallCommandlet} run.
   */
  @Test
  public void testUninstallMvnAndDotnetButDotNetNotInstalled() {

    // arrange
    String mvn = "mvn";
    String dotnet = "dotnet";
    IdeTestContext context = newContext(PROJECT_BASIC);
    CommandletManager commandletManager = context.getCommandletManager();
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Mvn mvnCommandlet = commandletManager.getCommandlet(Mvn.class);
    DotNet dotnetCommandlet = commandletManager.getCommandlet(DotNet.class);

    ToolProperty tools = uninstallCommandlet.tools;
    tools.addValue(mvnCommandlet);
    tools.addValue(dotnetCommandlet);

    // act
    uninstallCommandlet.run();
    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully uninstalled " + mvn),
        IdeLogEntry.ofWarning("Couldn't uninstall " + dotnet + " because we could not find an installation"));
    assertThat(context.getSoftwarePath().resolve("mvn")).doesNotExist();
  }

  @Test
  public void testUninstallEclipseFailsWhenNotInstalled() {

    // arrange
    String eclipse = "eclipse";
    IdeTestContext context = newContext(PROJECT_BASIC);
    CommandletManager commandletManager = context.getCommandletManager();
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
  public void testUninstallNode() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    CommandletManager commandletManager = context.getCommandletManager();
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Node nodeCommandlet = commandletManager.getCommandlet(Node.class);
    uninstallCommandlet.tools.addValue(nodeCommandlet);

    // act
    uninstallCommandlet.run();
    //assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully uninstalled node"));
  }

  /** Test of {@link UninstallCommandlet} run with --force. */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testForceUninstallAzure(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    String tool = "az";
    IdeTestContext context = newContext(PROJECT);
    context.setSystemInfo(systemInfo);
    mockInstallTool(context, tool);
    CommandletManager commandletManager = context.getCommandletManager();
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    Azure azureCommandlet = commandletManager.getCommandlet(Azure.class);
    uninstallCommandlet.tools.addValue(azureCommandlet);
    uninstallCommandlet.tools.setValueAsString(tool, context);
    context.getStartContext().setForceMode(true);

    // act
    uninstallCommandlet.run();
    // assert
    assertThat(context).log()
        .hasEntries(IdeLogEntry.ofSuccess("Successfully deleted " + context.getSoftwareRepositoryPath()
            .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve(tool).resolve("testVersion") + " from your computer."));
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully uninstalled " + tool));
    assertThat(context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve(tool).resolve("testVersion")).doesNotExist();
  }

  /** Test {@link UninstallCommandlet} without arguments uninstalls IDEasy. */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testUninstallIdeasy(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    IdeTestContext context = newContext("uninstall");
    context.setSystemInfo(systemInfo);
    CommandletManager commandletManager = context.getCommandletManager();
    UninstallCommandlet uninstallCommandlet = commandletManager.getCommandlet(UninstallCommandlet.class);
    context.getStartContext().setForceMode(true);
    WindowsHelper helper = context.getWindowsHelper();
    String originalPath = helper.getUserEnvironmentValue(IdeVariables.PATH.getName());
    if (systemInfo.isWindows()) {
      helper.setUserEnvironmentValue(IdeVariables.PATH.getName(), "C:\\projects\\_ide\\installation\\bin;" + originalPath);
    }
    // act
    uninstallCommandlet.run();
    // assert
    assertThat(context.getIdePath()).doesNotExist();
    assertThat(context.getUserHome().resolve(".bashrc")).hasContent("#already exists\n"
        + "alias devon=\"source ~/.devon/devon\"\n"
        + "devon\n"
        + "source ~/.devon/autocomplete\n");
    assertThat(context.getUserHome().resolve(".zshrc")).hasContent("#already exists\n"
        + "autoload -U +X bashcompinit && bashcompinit\n"
        + "alias devon=\"source ~/.devon/devon\"\n"
        + "devon\n"
        + "source ~/.devon/autocomplete\n");
    if (systemInfo.isWindows()) {
      assertThat(helper.getUserEnvironmentValue("IDE_ROOT")).isNull();
      assertThat(helper.getUserEnvironmentValue("PATH")).isEqualTo(
          "C:\\Users\\testuser\\AppData\\Local\\Microsoft\\WindowsApps;C:\\Users\\testuser\\scoop\\apps\\python\\current\\Scripts;C:\\Users\\testuser\\scoop\\apps\\python\\current;C:\\Users\\testuser\\scoop\\shims");
    }
  }
}
