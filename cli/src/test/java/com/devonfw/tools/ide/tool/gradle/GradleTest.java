package com.devonfw.tools.ide.tool.gradle;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link Gradle}.
 */
class GradleTest extends AbstractIdeContextTest {

  private static final String PROJECT_GRADLE = "gradle";
  private static final String GRADLE_VERSION = "8.7";

  /**
   * Tests the installation of {@link Gradle}
   *
   * @throws IOException if an I/O error occurs during the installation process
   */
  @Test
  void testGradleInstall() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_GRADLE);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("gradle", context);

    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests the execution of {@link Gradle}
   *
   * @throws IOException if an I/O error occurs during the installation process
   */
  @Test
  void testGradleRun() throws IOException {
    // arrange
    IdeTestContext context = newContext(PROJECT_GRADLE);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("gradle", context);
    Gradle commandLet = (Gradle) install.tool.getValue();
    commandLet.arguments.addValue("foo");
    commandLet.arguments.addValue("bar");

    // act
    commandLet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage("gradle " + "foo bar");
    checkInstallation(context);
  }

  /**
   * Tests if gradle run will use a gradle wrapper file if it was found within a valid cwd containing a build.gradle.
   */
  @Test
  void testGradleRunWithFoundWrapper() {
    // arrange
    IdeTestContext context = newContext(PROJECT_GRADLE);
    FileAccess fileAccess = context.getFileAccess();
    Gradle gradle = context.getCommandletManager().getCommandlet(Gradle.class);
    Path projectWithoutGradlew = context.getWorkspacePath().resolve("project-without-gradlew");
    fileAccess.mkdirs(projectWithoutGradlew);
    context.setCwd(projectWithoutGradlew, "main", context.getIdeHome());

    gradle.arguments.addValue("foo");
    gradle.arguments.addValue("bar");
    gradle.install();
    // create a build.gradle in a directory with no gradle wrapper file to trigger directory traversal
    fileAccess.touch(projectWithoutGradlew.resolve("build.gradle"));
    // copy the gradle wrapper file into the workspace
    fileAccess.copy(gradle.getToolBinPath().resolve("gradlew"), context.getWorkspacePath());
    // create a build.gradle next to the gradle wrapper file
    fileAccess.touch(context.getWorkspacePath().resolve("build.gradle"));

    // act
    gradle.run();

    // assert
    assertThat(context).logAtDebug().hasMessage("Using wrapper: " + context.getWorkspacePath().resolve("gradlew"));
    assertThat(context).logAtInfo().hasMessage("gradlew " + "foo bar");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    assertThat(context.getSoftwarePath().resolve("gradle/.ide.software.version")).exists().hasContent(GRADLE_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed gradle in version " + GRADLE_VERSION);

  }
}
