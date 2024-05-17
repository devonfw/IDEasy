package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Test of {@link BuildCommandlet}.
 */
public class BuildCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BUILD = "build";

  /**
   * Tests if the current working directory is being used if no path was provided.
   */
  @Test
  public void testUseCurrentWorkingDirectoryIfNoPathWasProvidedMvnBuild() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    buildCommandlet.path.setValue(null);
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.INFO, "No path was provided, using current working directory " + context.getCwd() + " as fallback.");
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.mvn.Mvn} build on a provided path.
   */
  @Test
  public void testMvnBuildWithProvidedPath() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    Path workspacePath = context.getWorkspacePath().resolve("mvn");
    buildCommandlet.path.setValue(workspacePath);
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed mvn in version 3.9.6");
    assertLogMessage(context, IdeLogLevel.INFO, "mvn " + workspacePath);
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.gradle.Gradle} build on a provided path.
   */
  @Test
  public void testGradleBuildWithProvidedPath() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    Path workspacePath = context.getWorkspacePath().resolve("gradle");
    buildCommandlet.path.setValue(workspacePath);
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed gradle in version 8.7");
    assertLogMessage(context, IdeLogLevel.INFO, "gradle " + workspacePath);
  }

  /**
   * Tests a {@link com.devonfw.tools.ide.tool.npm.Npm} build on a provided path.
   */
  @Test
  public void testNpmBuildWithProvidedPath() {

    IdeTestContext context = newContext(PROJECT_BUILD);
    BuildCommandlet buildCommandlet = context.getCommandletManager().getCommandlet(BuildCommandlet.class);
    Path workspacePath = context.getWorkspacePath().resolve("npm");
    buildCommandlet.path.setValue(workspacePath);
    buildCommandlet.run();
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed node in version v18.19.1");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed npm in version 9.9.2");
    assertLogMessage(context, IdeLogLevel.INFO, "npmcmdbin " + workspacePath);
  }
}
