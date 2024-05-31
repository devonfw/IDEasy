package com.devonfw.tools.ide.tool.jmc;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

/**
 * Integration test of {@link Jmc}.
 */
public class JmcTest extends AbstractIdeContextTest {

  private static final String PROJECT_JMC = "jmc";

  @Test
  public void testJmcInstallCommandlet() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jmc", context);
    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testJmcInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);

    Jmc commandlet = new Jmc(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testJmcRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Jmc commandlet = new Jmc(context);

    commandlet.arguments.addValue("foo");
    commandlet.arguments.addValue("bar");
    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "java jmc");
    assertLogMessage(context, IdeLogLevel.INFO, "jmc " + os + " foo bar");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/HelloWorld.txt")).hasContent("Hello World!");
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control")).doesNotExist();
    } else if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("jmc/jmc")).exists();
    }
    assertThat(context.getSoftwarePath().resolve("jmc/.ide.software.version")).exists().hasContent("8.3.0");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jmc in version 8.3.0");
  }
}
