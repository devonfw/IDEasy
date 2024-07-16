package com.devonfw.tools.ide.tool.jasypt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Integration test of {@link Jasypt}.
 */
@ExtendWith(SystemStubsExtension.class)
public class JasyptTest extends AbstractIdeContextTest {

  private static final String JASYPT_OPTS = "custom_argument";

  private static final String PROJECT_JASYPT = "jasypt";

  /**
   * Tests if {@link Jasypt} is properly installed by the {@link InstallCommandlet}
   */
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

  /**
   * Tests if {@link Jasypt} Commandlet installation is properly working
   */
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

  /**
   * Tests if {@link Jasypt} Commandlet is properly running
   */
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
    assertLogMessage(context, IdeLogLevel.INFO, context.getVariables().get("JASYPT_OPTS"));
    checkInstallation(context);
  }

  @SystemStub
  private final EnvironmentVariables environment = new EnvironmentVariables();

  /**
   * Tests if {@link Jasypt} Commandlet is properly running with a user-defined JASYPT_OPTS env variable
   */
  @Test
  public void testJasyptRunWithCustomVariable() {

    // arrange
    this.environment.set("JASYPT_OPTS", JASYPT_OPTS);

    IdeTestContext context = newContext(PROJECT_JASYPT);
    Jasypt commandlet = new Jasypt(context);

    commandlet.command.setValue(JasyptCommand.ENCRYPT);
    commandlet.masterPassword.setValue("password");
    commandlet.secret.setValue("input");

    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, context.getVariables().get("JASYPT_OPTS"));

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
