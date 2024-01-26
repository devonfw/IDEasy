package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

/** Integration test of {@link EditionGetCommandlet}. */

public class EditionGetCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link VersionGetCommandlet} run. */
  @Test
  public void testEditionGetCommandletRun() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    String tool = "az";
    IdeTestContext context = newContext("basic", path, true);
    mockInstallTool(context, tool);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);

    // act
    editionGet.tool.setValueAsString(tool, context);
    editionGet.run();

    // assert
    List<String> logs = context.level(IdeLogLevel.INFO).getMessages();
    assertThat(logs).contains("testEdition");
  }

  /**
   * Mocks the installation of a tool, since getEdition depends on symlinks which are not distributed with git
   *
   * @param context the {@link IdeContext} to use.
   * @param tool the tool to mock install.
   */
  private static void mockInstallTool(IdeTestContext context, String tool) {

    Path pathToInstallationOfDummyTool = context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve("testEdition/testVersion");
    Path pathToLinkedSoftware = context.getSoftwarePath().resolve(tool);
    context.getFileAccess().symlink(pathToInstallationOfDummyTool, pathToLinkedSoftware);
  }

  /** Test of {@link VersionGetCommandlet} run, when Installed Version is null. */
  @Test
  public void testVersionGetCommandletRunThrowsCliException() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("java", context);
    // act
    try {
      editionGet.run();
      failBecauseExceptionWasNotThrown(CliException.class);
    } catch (CliException e) {
      // assert
      assertThat(e).hasMessageContaining("Tool java is not installed!");
    }
  }
}
