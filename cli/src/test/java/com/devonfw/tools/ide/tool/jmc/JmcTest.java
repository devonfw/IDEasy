package com.devonfw.tools.ide.tool.jmc;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Integration test of {@link Jmc}.
 */
public class JmcTest extends AbstractIdeContextTest {

  private static final String PROJECT_JMC = "jmc";

  @Test
  public void jmcPostInstallShouldMoveFilesIfRequiredMockedServer() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jmc", context);
    // act
    install.run();

    // assert
    performPostInstallAssertion(context);
  }

  @Test
  public void jmcPostInstallShouldMoveFilesIfRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);

    Jmc commandlet = new Jmc(context);

    // act
    commandlet.install();

    // assert
    performPostInstallAssertion(context);
  }

  @Test
  @Disabled("TODO: not yet completed and invocation of dummy java not working as expected.")
  public void jmcShouldRunExecutableSuccessful() {

    // arrange
    //Path mockResultPath = Path.of("target/test-projects/java/project");

    IdeTestContext context = newContext(PROJECT_JMC);
    Jmc commandlet = new Jmc(context);
    commandlet.arguments.setValue(List.of("foo", "bar"));
    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "java jmc");
    assertLogMessage(context, IdeLogLevel.INFO, "jmc linux foo bar");
    //assertThat(context.getIdeHome().resolve("jmc.log")).exists().hasContent(expectedOutput);
  }

  private void performPostInstallAssertion(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/HelloWorld.txt")).hasContent("Hello World!");
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control")).doesNotExist();
    } else if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control.app/Contents")).exists();
    }
    assertThat(context.getSoftwarePath().resolve("jmc/.ide.software.version")).exists().hasContent("8.3.0");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jmc in version 8.3.0");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jmc in version 8.3.0");
  }

}
