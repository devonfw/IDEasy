package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/** Integration test of {@link EditionGetCommandlet}. */

public class EditionGetCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link EditionGetCommandlet} run. */
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
    assertThat(context).log().hasMessage(tool);
  }

  /**
   * Mocks the installation of a tool, since getEdition depends on symlinks which are not distributed with git
   *
   * @param context the {@link IdeContext} to use.
   * @param tool the tool to mock install.
   */
  private static void mockInstallTool(IdeTestContext context, String tool) {

    Path pathToInstallationOfDummyTool = context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve(tool + "/testVersion");
    Path pathToLinkedSoftware = context.getSoftwarePath().resolve(tool);
    context.getFileAccess().symlink(pathToInstallationOfDummyTool, pathToLinkedSoftware);
  }

  /** Test of {@link VersionGetCommandlet} run with --configured flag */
  @Test
  public void testEditionGetCommandletConfiguredEdition() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("java", context);
    editionGet.configured.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("java");
  }

  /** Test of {@link EditionGetCommandlet} run, when tool is not installed. */
  @Test
  public void testEditionGetCommandletToolNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    // act
    editionGet.tool.setValueAsString("az", context);
    editionGet.run();
    // assert
    assertThat(context).logAtInfo().hasEntries("No installation of tool az was found.",
        "The configured edition for tool az is az",
        "To install that edition call the following command:",
        "ide install az");
  }

  /** Test of {@link EditionGetCommandlet} run, with --installed flag, when Installed Version is null. */
  @Test
  public void testEditionGetCommandletInstalledEditionToolNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("java", context);
    editionGet.installed.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).logAtInfo().hasEntries("No installation of tool java was found.",
        "The configured edition for tool java is java",
        "To install that edition call the following command:",
        "ide install java");
  }


  /** Test of {@link EditionGetCommandlet} run, with --installed flag, when tool is installed. */
  @Test
  public void testEditionGetCommandletInstalledEditionToolInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    // act
    editionGet.tool.setValueAsString("mvn", context);
    editionGet.installed.setValue(true);
    editionGet.run();
    // assert
    assertThat(context).logAtInfo().hasMessage("mvn");
  }
}
