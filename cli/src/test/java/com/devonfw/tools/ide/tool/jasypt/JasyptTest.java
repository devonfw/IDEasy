package com.devonfw.tools.ide.tool.jasypt;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Jasypt}.
 */
class JasyptTest extends AbstractIdeContextTest {

  private static final String JASYPT_OPTS = "custom_argument";

  private static final String PROJECT_JASYPT = "jasypt";

  /**
   * Tests if {@link Jasypt} is properly installed by the {@link InstallCommandlet}
   */
  @Test
  void testJasyptInstallCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jasypt", context);
    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if {@link Jasypt} Commandlet installation is properly working
   */
  @Test
  void testJasyptInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);

    Jasypt commandlet = new Jasypt(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if {@link Jasypt} Commandlet is properly running
   */
  @Test
  void testJasyptRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    Jasypt commandlet = new Jasypt(context);

    commandlet.command.setValue(JasyptCommand.ENCRYPT);
    commandlet.masterPassword.setValue("password");
    commandlet.secret.setValue("input");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage(context.getVariables().get("JASYPT_OPTS"));
    checkInstallation(context);
  }

  /**
   * Tests if {@link Jasypt} Commandlet is properly running with a user-defined JASYPT_OPTS env variable
   */
  @Test
  void testJasyptRunWithCustomVariable() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    context.getSystem().setEnv("JASYPT_OPTS", JASYPT_OPTS);
    Jasypt commandlet = new Jasypt(context);

    commandlet.command.setValue(JasyptCommand.ENCRYPT);
    commandlet.masterPassword.setValue("password");
    commandlet.secret.setValue("input");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage(context.getVariables().get("JASYPT_OPTS"));

    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    // install - java
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    // commandlet - jasypt
    assertThat(context.getSoftwarePath().resolve("jasypt/jasypt-1.9.3.jar")).hasContent("This is a jar file.");
    assertThat(context.getSoftwarePath().resolve("jasypt/.ide.software.version")).exists().hasContent("1.9.3");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed jasypt in version 1.9.3");
  }
}
