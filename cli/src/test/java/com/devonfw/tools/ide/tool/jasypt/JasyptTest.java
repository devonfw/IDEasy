package com.devonfw.tools.ide.tool.jasypt;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Integration test of {@link Jasypt}.
 */
public class JasyptTest extends AbstractIdeContextTest {

  private static final String PROJECT_JMC = "jasypt";

  @Test
  public void testJasyptInstallCommandlet() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);
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
    IdeTestContext context = newContext(PROJECT_JMC);

    Jasypt commandlet = new Jasypt(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testJasyptRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_JMC);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Jasypt commandlet = new Jasypt(context);
    commandlet.arguments.setValue(List.of("foo", "bar"));
    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "java jasypt");
    assertLogMessage(context, IdeLogLevel.INFO, "jasypt " + os + " foo bar");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jasypt/HelloWorld.txt")).hasContent("Hello World!");
      assertThat(context.getSoftwarePath().resolve("jasypt/JDK Mission Control")).doesNotExist();
    } else if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("jasypt/jasypt")).exists();
    }
    assertThat(context.getSoftwarePath().resolve("jasypt/.ide.software.version")).exists().hasContent("8.3.0");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jasypt in version 8.3.0");
  }

}
