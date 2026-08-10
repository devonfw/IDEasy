package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Gui}.
 */
class GuiTest extends AbstractIdeContextTest {

  private static ToolInstallation newMvnInstallation(Path binDir) {

    return new ToolInstallation(binDir, binDir, binDir, VersionIdentifier.of("3.9.16"), false);
  }

  /**
   * Test that {@link Gui} does not require {@code IDE_HOME}. The GUI is launched from a desktop shortcut, which always starts outside of any IDEasy project.
   * Project selection happens inside the GUI, which reads the available projects from {@code IDE_ROOT}. See
   * <a href="https://github.com/devonfw/IDEasy/issues/2279">#2279</a> for reference.
   */
  @Test
  void testThatIdeHomeIsNotRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Gui gui = new Gui(context);
    // act & assert
    assertThat(gui.isIdeHomeRequired()).isFalse();
  }

  /**
   * Test that {@link Gui} still requires {@code IDE_ROOT}, since the GUI resolves both the launcher {@code pom.xml} and the list of projects relative to it.
   */
  @Test
  void testThatIdeRootIsRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Gui gui = new Gui(context);
    // act & assert
    assertThat(gui.isIdeRootRequired()).isTrue();
  }

  /**
   * Test that {@link Gui#registerMvnBinDir(Mvn, ToolInstallation)} registers the bin directory without {@code IDE_HOME}. Without a project there is no software
   * folder to resolve Maven from, so the binary would not be found at all.
   */
  @Test
  void testThatMvnBinDirIsRegisteredWithoutIdeHome() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setIdeHome(null);
    Gui gui = new Gui(context);
    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);
    Path binDir = context.getIdeRoot().resolve("software/mvn/bin");
    assertThat(context.getPath().getPath(mvn.getName())).as("Maven is unknown without IDE_HOME").isNull();
    // act
    gui.registerMvnBinDir(mvn, newMvnInstallation(binDir));
    // assert
    assertThat(context.getPath().getPath(mvn.getName())).isEqualTo(binDir);
  }

  /**
   * Test that {@link Gui#registerMvnBinDir(Mvn, ToolInstallation)} leaves the tool map untouched inside a project. The GUI installs the latest Maven, so
   * registering it would override the version configured for the project.
   */
  @Test
  void testThatMvnBinDirIsNotRegisteredInsideProject() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Gui gui = new Gui(context);
    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);
    Path configuredMvn = context.getPath().getPath(mvn.getName());
    assertThat(configuredMvn).as("Maven of the project is known inside a project").isNotNull();
    // act
    gui.registerMvnBinDir(mvn, newMvnInstallation(context.getIdeRoot().resolve("latest-mvn/bin")));
    // assert
    assertThat(context.getPath().getPath(mvn.getName())).isEqualTo(configuredMvn);
  }

  /**
   * Test that {@link Gui#registerMvnBinDir(Mvn, ToolInstallation)} ignores a {@code null} bin directory. A {@code null} value in the tool map would break the
   * next binary lookup, since {@code SystemPath} resolves against the stored path without a null check.
   */
  @Test
  void testThatNullBinDirIsNotRegistered() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setIdeHome(null);
    Gui gui = new Gui(context);
    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);
    // act
    gui.registerMvnBinDir(mvn, newMvnInstallation(null));
    // assert
    // asserting on getPath alone would not distinguish "not registered" from "registered as null", and asserting on the resolved path would depend on the
    // machine running the test, so we verify the actual failure mode: a null value in the tool map makes the next lookup fail
    assertThatCode(() -> context.getPath().findBinary(Path.of(mvn.getName()))).doesNotThrowAnyException();
  }
}
