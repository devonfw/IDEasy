package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link BuildCommandlet}.
 */
public class BuildCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BUILD = "build";

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.mvn.Mvn} build with provided arguments.
   */
  @Test
  public void testMvnBuildWithProvidedArguments() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("mvn"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.arguments.addValue("clean");
    buildCommandlet.arguments.addValue("install");
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed mvn in version 3.9.6");
    assertLogMessage(context, IdeLogLevel.INFO, "mvn clean install");
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.gradle.Gradle} build with provided arguments.
   */
  @Test
  public void testGradleBuildWithProvidedArguments() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("gradle"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.arguments.addValue("task1");
    buildCommandlet.arguments.addValue("task2");
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed gradle in version 8.7");
    assertLogMessage(context, IdeLogLevel.INFO, "gradle task1 task2");
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.npm.Npm} build with provided arguments.
   */
  @Test
  public void testNpmBuildWithProvidedArguments() {

    SystemInfo systemInfo = SystemInfoMock.of("linux");
    IdeTestContext context = newContext(PROJECT_BUILD);
    context.setSystemInfo(systemInfo);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("npm"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.arguments.addValue("start");
    buildCommandlet.arguments.addValue("test");
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed node in version v18.19.1");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed npm in version 9.9.2");
    assertLogMessage(context, IdeLogLevel.INFO, "npm start test");
  }
}
