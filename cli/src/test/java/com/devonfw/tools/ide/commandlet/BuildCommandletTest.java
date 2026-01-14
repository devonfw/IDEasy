package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link BuildCommandlet}.
 */
@WireMockTest
class BuildCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BUILD = "build";

  private static final String JAVA_VERSION = "17.0.10_7";

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.mvn.Mvn} build without arguments and expects defaults to be taken from ide.properties.
   */
  @Test
  void testMvnBuildWithoutProvidedArgumentsUsesDefaultOptions() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("mvn"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.run();
    assertThat(context).log().hasEntries(IdeLogEntry.ofDebug("Tool mvn has 1 other tool(s) as dependency"),
        IdeLogEntry.ofSuccess("Successfully installed java in version " + JAVA_VERSION),
        IdeLogEntry.ofSuccess("Successfully installed mvn in version 3.9.6"),
        IdeLogEntry.ofInfo("mvn clean compile"));
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.mvn.Mvn} build with provided arguments.
   */
  @Test
  void testMvnBuildWithProvidedArguments() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("mvn"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.arguments.addValue("clean");
    buildCommandlet.arguments.addValue("install");
    buildCommandlet.run();
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed java in version " + JAVA_VERSION),
        IdeLogEntry.ofSuccess("Successfully installed mvn in version 3.9.6"),
        IdeLogEntry.ofInfo("mvn clean install"));
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.gradle.Gradle} build with provided arguments.
   */
  @Test
  void testGradleBuildWithProvidedArguments() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("gradle"), context.getWorkspacePath().toString(), context.getIdeHome());
    buildCommandlet.arguments.addValue("task1");
    buildCommandlet.arguments.addValue("task2");
    buildCommandlet.run();
    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("Successfully installed java in version " + JAVA_VERSION),
        IdeLogEntry.ofSuccess("Successfully installed gradle in version 8.7"),
        IdeLogEntry.ofInfo("gradle task1 task2"));
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.npm.Npm} build with provided arguments.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmBuildWithProvidedArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    SystemInfo systemInfo = SystemInfoMock.of("linux");
    IdeTestContext context = newContext(PROJECT_BUILD, wireMockRuntimeInfo);
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
  void testBuildWithNoCwd() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(null, context.getWorkspacePath().toString(), context.getIdeHome());
    assertThrows(CliException.class, buildCommandlet::run);
  }

  /**
   * Tests a build with an empty workspace.
   */
  @Test
  void testBuildWithNoBuildFile() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    context.setCwd(context.getWorkspacePath().resolve("empty"), context.getWorkspacePath().toString(), context.getIdeHome());
    assertThrows(CliException.class, buildCommandlet::run);
  }
}
