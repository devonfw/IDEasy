package com.devonfw.tools.ide.commandlet;

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
    String tool = "az";
    IdeTestContext context = newContext(PROJECT_BASIC);
    mockInstallTool(context, tool);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);

    // act
    editionGet.tool.setValueAsString(tool, context);
    editionGet.run();

    // assert
    List<String> logs = context.level(IdeLogLevel.INFO).getMessages();
    assertThat(logs).contains("az");
  }

  /**
   * Mocks the installation of a tool, since getEdition depends on symlinks which are not distributed with git
   *
   * @param context the {@link IdeContext} to use.
   * @param tool the tool to mock install.
   */
  private static void mockInstallTool(IdeTestContext context, String tool) {

    Path pathToInstallationOfDummyTool = context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve("az/testVersion");
    Path pathToLinkedSoftware = context.getSoftwarePath().resolve(tool);
    context.getFileAccess().symlink(pathToInstallationOfDummyTool, pathToLinkedSoftware);
  }

  /** Test of {@link VersionGetCommandlet} run, when Installed Version is null. */
  @Test
  public void testVersionGetCommandletRunPrintConfiguredEdition() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("java", context);
    // act
    editionGet.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "The configured edition for tool java is java");
    assertLogMessage(context, IdeLogLevel.INFO, "To install that edition call the following command:");
    assertLogMessage(context, IdeLogLevel.INFO, "ide install java");
  }
}
