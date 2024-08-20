package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link BuildCommandlet}.
 */
public class BuildCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BUILD = "build";

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.mvn.Mvn} build without arguments and expects defaults to be taken from ide.properties.
   */
  @Test
  public void testMvnBuildWithoutProvidedArgumentsUsesDefaultOptions() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("mvn"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.run();
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed java in version 17.0.10_7"),
        IdeLogEntry.ofSuccess("Successfully installed mvn in version 3.9.6"),
        IdeLogEntry.ofInfo("mvn clean compile"));
  }

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
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed java in version 17.0.10_7"),
        IdeLogEntry.ofSuccess("Successfully installed mvn in version 3.9.6"),
        IdeLogEntry.ofInfo("mvn clean install"));
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
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed java in version 17.0.10_7"),
        IdeLogEntry.ofSuccess("Successfully installed gradle in version 8.7"),
        IdeLogEntry.ofInfo("gradle task1 task2"));
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
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed node in version v18.19.1"),
        IdeLogEntry.ofSuccess("Successfully installed npm in version 9.9.2"),
        IdeLogEntry.ofInfo("npm start test"));
  }

  /**
   * Tests a build with no cwd.
   */
  @Test
  public void testBuildWithNoCwd() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(null, context.getWorkspacePath().toString(), context.getIdeHome());
    assertThrows(CliException.class, () -> buildCommandlet.run());
  }

  /**
   * Tests a build with an empty workspace.
   */
  @Test
  public void testBuildWithNoBuildFile() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("empty"), context.getWorkspacePath().toString(), context.getIdeHome());
    assertThrows(CliException.class, () -> buildCommandlet.run());
  }
}
