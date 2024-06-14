package com.devonfw.tools.ide.tool.mvn;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Integration test of {@link Mvn}.
 */
public class MvnTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  @Test
  public void testMvnInstallCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    context.setInputValues(List.of("value1", "value2"));
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("mvn", context);

    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testMvnRun() {
    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    context.setInputValues(List.of("value1", "value2"));
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("mvn", context);
    Mvn commandlet = (Mvn) install.tool.getValue();
    commandlet.arguments.addValue("foo");
    commandlet.arguments.addValue("bar");

    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "mvn " + "foo bar");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    assertThat(context.getSoftwarePath().resolve("mvn/.ide.software.version")).exists().hasContent("3.9.7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed mvn in version 3.9.7");
    assertThat(context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_FILE)).exists();
    assertThat(context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_SECURITY_FILE)).exists();
  }
}
