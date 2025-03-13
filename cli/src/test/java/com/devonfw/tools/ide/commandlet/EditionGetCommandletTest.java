package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/** Integration test of {@link EditionGetCommandlet}. */

public class EditionGetCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT = "edition-version-get-uninstall";

  /**
   * Mocks the installation of a tool, since getEdition depends on symlinks which are not distributed with git
   *
   * @param context the {@link IdeContext} to use.
   * @param tool the tool to mock installation of.
   */
  private static void mockInstallTool(IdeTestContext context, String tool) {

    Path pathToInstallationOfDummyTool = context.getSoftwareRepositoryPath()
        .resolve(context.getDefaultToolRepository().getId()).resolve(tool).resolve(tool + "/testVersion");
    Path pathToLinkedSoftware = context.getSoftwarePath().resolve(tool);
    context.getFileAccess().symlink(pathToInstallationOfDummyTool, pathToLinkedSoftware);
  }

  /** Test of {@link EditionGetCommandlet} run. */
  @Test
  public void testEditionGetCommandletRunWithoutFlagsAndNotInstalled() {

    // arrange
    String tool = "az";
    IdeTestContext context = newContext(PROJECT, null, true);
    mockInstallTool(context, tool);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString(tool, context);
    // act
    editionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofWarning("Undefined edition " + tool + " of tool " + tool),
        IdeLogEntry.ofProcessable("The installed edition for tool " + tool + " is " + tool),
        IdeLogEntry.ofProcessable("The configured edition for tool " + tool + " is " + tool));
  }

  /** Test of {@link VersionGetCommandlet} run with --configured flag and installed tool */
  @Test
  public void testEditionGetCommandletConfiguredEditionAndInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("java", context);
    editionGet.configured.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("java");
  }

  /** Test of {@link VersionGetCommandlet} run with --configured flag and not installed tool but set 'basic' edition for TOMCAT_EDITION in ide.properties */
  @Test
  public void testEditionGetCommandletNotInstalledConfiguredEdition() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("tomcat", context);
    editionGet.configured.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("basic");
  }

  /** Test of {@link VersionGetCommandlet} run with --configured flag but tool was not installed */
  @Test
  public void testEditionGetCommandletConfiguredEditionButNotInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("az", context);
    editionGet.configured.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("az");
  }

  /** Test of {@link EditionGetCommandlet} run, when tool is not installed but --installed flag was used and --configured was not used. */
  @Test
  public void testEditionGetCommandletToolNotInstalledButInstalledFlagInUseThrowsException() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("az", context);
    editionGet.installed.setValue(true);
    // act/assert
    assertThrows(CliException.class, () -> editionGet.run());
  }

  /** Test of {@link EditionGetCommandlet} run, with --installed flag, when tool is installed. */
  @Test
  public void testEditionGetCommandletInstalledEditionToolInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString("mvn", context);
    editionGet.installed.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).log(IdeLogLevel.PROCESSABLE).hasMessage("mvn");
  }

  /** Test of {@link EditionGetCommandlet} run, with --installed and --configured flag, when tool is not installed. */
  @Test
  public void testEditionGetCommandletInstalledConfiguredEditionToolNotInstalled() {

    // arrange
    String tool = "tomcat";
    IdeTestContext context = newContext(PROJECT, null, false);
    EditionGetCommandlet editionGet = context.getCommandletManager().getCommandlet(EditionGetCommandlet.class);
    editionGet.tool.setValueAsString(tool, context);
    editionGet.configured.setValue(true);
    editionGet.installed.setValue(true);
    // act
    editionGet.run();
    // assert
    assertThat(context).log().hasEntries(
        IdeLogEntry.ofProcessable("No installation of tool " + tool + " was found."),
        IdeLogEntry.ofProcessable("The configured edition for tool " + tool + " is basic"),
        IdeLogEntry.ofProcessable("ide install " + tool)
    );
  }
}
