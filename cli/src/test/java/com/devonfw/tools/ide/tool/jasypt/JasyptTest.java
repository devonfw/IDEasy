package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

/**
 * Integration test of {@link Jasypt}.
 */
public class JasyptTest extends AbstractIdeContextTest {

  private static final String PROJECT_JASYPT = "jasypt";

  @Test
  public void testJasyptInstallCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jasypt", context);
    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testJasyptInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);

    Jasypt commandlet = new Jasypt(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testJasyptRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    Jasypt commandlet = new Jasypt(context);

    commandlet.command.setValue(JasyptCommand.ENCRYPT);
    commandlet.masterPassword.setValue("password");
    commandlet.secret.setValue("input");

    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "executing java:");
    assertLogMessage(context, IdeLogLevel.INFO, "This is a jar file.");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    // install - java
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    // commandlet - jasypt
    assertThat(context.getSoftwarePath().resolve("jasypt/jasypt-1.9.3.jar")).hasContent("This is a jar file.");
    assertThat(context.getSoftwarePath().resolve("jasypt/.ide.software.version")).exists().hasContent("1.9.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jasypt in version 1.9.3");
  }
}
