package com.devonfw.tools.ide.tool.gradle;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link Gradle}.
 */
public class GradleTest extends AbstractIdeContextTest {

  private static final String PROJECT_GRADLE = "gradle";
  private static final String GRADLE_VERSION = "8.7";

  /**
   * Tests the installation of {@link Gradle}
   *
   * @throws IOException if an I/O error occurs during the installation process
   */
  @Test
  public void testgradleInstall() throws IOException {

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
  public void testGradleRun() throws IOException {
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

  private void checkInstallation(IdeTestContext context) throws IOException {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    assertThat(context.getSoftwarePath().resolve("gradle/.ide.software.version")).exists().hasContent(GRADLE_VERSION);
    assertThat(context).logAtSuccess().hasMessage("Successfully installed gradle in version " + GRADLE_VERSION);

  }
}
