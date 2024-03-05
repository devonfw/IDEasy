package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Integration test of {@link Jasypt}.
 */
public class JasyptTest extends AbstractIdeContextTest {

  private static final String PROJECT_JASYPT = "jasypt";

  @Test
  public void testJasypt() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JASYPT);
    Jasypt commandlet = new Jasypt(context);

    // act install
    commandlet.install();

    // assert install
    assertLogMessage(context, IdeLogLevel.INFO, "executing mvn:"); // assert postInstall()
    checkInstallation(context);

    // act and assert run
    runNoArgs(context, commandlet, null);
    runRightArgs(context, commandlet, List.of("encrypt", "master", "secret"));
    runRightArgs(context, commandlet, List.of("decrypt", "master", "secret"));
    runWrongArgs(context, commandlet, List.of("wrong args"));
  }

  public void runNoArgs(IdeTestContext context, Jasypt commandlet, List<String> arguments) {

    String expectedMessage = """
        Jasypt encryption tool
        Usage:
         encrypt  <masterpassword>  <secret>             encrypt a secret with a master-password
         decrypt  <masterpassword>  <secret>             decrypt an encrypted secret with a master-password
        """;
    commandlet.arguments.setValue(arguments);
    commandlet.run();
    assertLogMessage(context, IdeLogLevel.INFO, expectedMessage);
  }

  public void runWrongArgs(IdeTestContext context, Jasypt commandlet, List<String> arguments) {

    commandlet.arguments.setValue(arguments);
    commandlet.run();
    assertLogMessage(context, IdeLogLevel.WARNING, "Unknown arguments");
  }

  public void runRightArgs(IdeTestContext context, Jasypt commandlet, List<String> arguments) {

    commandlet.arguments.setValue(arguments);
    commandlet.run();
    assertLogMessage(context, IdeLogLevel.INFO, "executing java:");
  }

  private void checkInstallation(IdeTestContext context) {

    // dependencies
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    assertThat(context.getSoftwarePath().resolve("mvn/bin/mvn")).exists();

    // commandlet
    assertThat(context.getSoftwarePath().resolve("jasypt/META-INF/HelloWorld.txt")).hasContent("Hello World!");
    assertThat(context.getSoftwarePath().resolve("jasypt/org/HelloWorld.txt")).hasContent("Hello World!");
    assertThat(context.getSoftwarePath().resolve("jasypt/.ide.software.version")).exists().hasContent("1.9.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed jasypt in version 1.9.3");
  }

}
