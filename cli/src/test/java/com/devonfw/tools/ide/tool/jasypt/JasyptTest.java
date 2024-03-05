package com.devonfw.tools.ide.tool.jasypt;

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
import java.util.List;

/**
 * Integration test of {@link Jasypt}.
 */
public class JasyptTest extends AbstractIdeContextTest {

  private static final String PROJECT_JASYPT = "jasypt";

  @Test
  public void testJasyptInstallCommandlet() throws IOException {

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
    //SystemInfo systemInfo = SystemInfoMock.of(os);
   // context.setSystemInfo(systemInfo);
    Jasypt commandlet = new Jasypt(context);
    commandlet.arguments.setValue(List.of("foo", "bar"));
    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "java jasypt");
    //assertLogMessage(context, IdeLogLevel.INFO, "jasypt " + os + " foo bar");
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    assertThat(context.getSoftwarePath().resolve("mvn/bin/mvn")).exists();

    assertThat(context.getSoftwarePath().resolve("jasypt/META-INF/HelloWorld.txt")).hasContent("Hello World!");
    assertThat(context.getSoftwarePath().resolve("jasypt/org/HelloWorld.txt")).hasContent("Hello World!");
    assertThat(context.getSoftwarePath().resolve("jasypt/JDK Mission Control")).doesNotExist();

    assertThat(context.getSoftwarePath().resolve("jasypt/.ide.software.version")).exists().hasContent("1.9.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jasypt in version 1.9.3");
  }

}
