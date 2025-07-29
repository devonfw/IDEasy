package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link IdeContext}.
 */
public class IdeContextTest extends AbstractIdeContextTest {

  /**
   * Test of {@link IdeContext} initialization from basic project.
   */
  @Test
  public void testBasicProjectEnvironment() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    // act
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    // assert
    assertThat(context.getWorkspaceName()).isEqualTo("foo-test");
    assertThat(IdeVariables.DOCKER_EDITION.get(context)).isEqualTo("docker");
    EnvironmentVariables variables = context.getVariables();
    assertThat(variables.get("FOO")).isEqualTo("foo-bar-some-${UNDEFINED}");
    assertThat(context).logAtWarning().hasMessage("Undefined variable ${UNDEFINED} in 'FOO' at 'SOME=some-${UNDEFINED}'");
    assertThat(context.getIdeHome().resolve("readme")).hasContent("this is the IDE_HOME directory");
    assertThat(context.getIdeRoot().resolve("readme")).hasContent("this is the IDE_ROOT directory");
    assertThat(context.getUserHome().resolve("readme")).hasContent("this is the users HOME directory");
    assertThat(variables.getPath("M2_REPO")).isEqualTo(context.getUserHome().resolve(".m2/repository"));
    assertThat(context.getDownloadPath().resolve("readme")).hasContent("this is the download cache");
    assertThat(context.getUrlsPath().resolve("readme")).hasContent("this is the download metadata");
    assertThat(context.getToolRepositoryPath().resolve("readme")).hasContent("this is the tool repository");
    assertThat(context.getWorkspacePath().resolve("readme")).hasContent("this is the foo-test workspace of basic");
    SystemPath systemPath = IdeVariables.PATH.get(context);
    assertThat(systemPath).isSameAs(context.getPath());
    String javaPath = context.getSoftwarePath().resolve("java").resolve("bin").toString();
    assertThat(systemPath.toString()).isNotEqualTo(javaPath).contains(javaPath);
    Path softwarePath = context.getSoftwarePath();
    Path javaBin = softwarePath.resolve("java/bin");
    assertThat(systemPath.getPath("java")).isEqualTo(javaBin);
    Path mvnBin = softwarePath.resolve("mvn/bin");
    assertThat(systemPath.getPath("mvn")).isEqualTo(mvnBin);
    assertThat(systemPath.toString()).contains(javaBin.toString(), mvnBin.toString());
    assertThat(variables.getType()).isSameAs(EnvironmentVariablesType.RESOLVED);
    assertThat(variables.getByType(EnvironmentVariablesType.RESOLVED)).isSameAs(variables);
    EnvironmentVariables v1 = variables.getParent();
    assertThat(v1.getType()).isSameAs(EnvironmentVariablesType.CONF);
    assertThat(variables.getByType(EnvironmentVariablesType.CONF)).isSameAs(v1);
    EnvironmentVariables v2 = v1.getParent();
    assertThat(v2.getType()).isSameAs(EnvironmentVariablesType.WORKSPACE);
    assertThat(variables.getByType(EnvironmentVariablesType.WORKSPACE)).isSameAs(v2);
    EnvironmentVariables v3 = v2.getParent();
    assertThat(v3.getType()).isSameAs(EnvironmentVariablesType.SETTINGS);
    assertThat(variables.getByType(EnvironmentVariablesType.SETTINGS)).isSameAs(v3);
    EnvironmentVariables v4 = v3.getParent();
    assertThat(v4.getType()).isSameAs(EnvironmentVariablesType.USER);
    assertThat(variables.getByType(EnvironmentVariablesType.USER)).isSameAs(v4);
    EnvironmentVariables v5 = v4.getParent();
    assertThat(v5.getType()).isSameAs(EnvironmentVariablesType.SYSTEM);
    assertThat(variables.getByType(EnvironmentVariablesType.SYSTEM)).isSameAs(v5);
    assertThat(v5.getParent()).isNull();
  }

  /**
   * Tests if the user is starting IDEasy from the "workspaces" directory, IDEasy should fall back to the "main" workspace.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/466">#466</a>
   */
  @Test
  public void testWorkspacePathFallsBackToMainWorkspace() {
    // arrange
    String path = "project/workspaces";
    // act
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    Path workspacePath = context.getWorkspacePath();
    String workspaceName = context.getWorkspaceName();
    // assert
    assertThat(IdeVariables.WORKSPACE_PATH.get(context)).isEqualTo(workspacePath);
    assertThat(IdeVariables.WORKSPACE.get(context)).isEqualTo(workspaceName);
    assertThat(workspacePath).isEqualTo(TEST_PROJECTS.resolve(PROJECT_BASIC).resolve(path).resolve("main").toAbsolutePath());
    assertThat(workspaceName).isEqualTo("main");
  }

  /**
   * Tests if the user is starting IDEasy from the "project" directory, IDEasy should fall back to the "main" workspace.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/466">#466</a>
   */
  @Test
  public void testProjectPathFallsBackToMainWorkspace() {
    // arrange
    String path = "project";
    // act
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    Path workspacePath = context.getWorkspacePath();
    String workspaceName = context.getWorkspaceName();
    // assert
    assertThat(IdeVariables.WORKSPACE_PATH.get(context)).isEqualTo(workspacePath);
    assertThat(IdeVariables.WORKSPACE.get(context)).isEqualTo(workspaceName);
    assertThat(workspacePath).isEqualTo(TEST_PROJECTS.resolve(PROJECT_BASIC).resolve(path).resolve("workspaces").resolve("main").toAbsolutePath());
    assertThat(workspaceName).isEqualTo("main");
  }

  /**
   * Tests if the user is starting IDEasy within a workspace directory, IDEasy should use this directory as the current workspace.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/466">#466</a>
   */
  @Test
  public void testCurrentWorkspacePathIsUsed() {
    // arrange
    String path = "project/workspaces/foo-test";
    // act
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    Path workspacePath = context.getWorkspacePath();
    String workspaceName = context.getWorkspaceName();
    // assert
    assertThat(IdeVariables.WORKSPACE_PATH.get(context)).isEqualTo(workspacePath);
    assertThat(IdeVariables.WORKSPACE.get(context)).isEqualTo(workspaceName);
    assertThat(workspacePath).isEqualTo(TEST_PROJECTS.resolve(PROJECT_BASIC).resolve(path).toAbsolutePath());
    assertThat(workspaceName).isEqualTo("foo-test");
  }

  // hier test einfügen mit idetestcontext
  @Test
  public void testIdeVersionTooSmall() {
    // arrange
    String path = "project/workspaces/foo-test";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentVariables variables = context.getVariables();
    String ideMinVersion = String.valueOf(Integer.MAX_VALUE);
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_MIN_VERSION", ideMinVersion);
    CliArguments args = new CliArguments();
    String warningMessage = String.format("Your version of IDEasy is currently %s\n"
        + "However, this is too old as your project requires at latest version %s\n"
        + "Please run the following command to update to the latest version of IDEasy and fix the problem:\n"
        + "ide upgrade", IdeVersion.getVersionIdentifier().toString(), ideMinVersion);
    // act
    context.run(args);
    // assert
    assertThat(context).logAtWarning().hasMessage(warningMessage);
  }

  @Test
  public void testIdeVersionOk() {
    // arrange
    String path = "project/workspaces/foo-test";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentVariables variables = context.getVariables();
    String ideVersion = IdeVersion.getVersionIdentifier().toString();
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_MIN_VERSION", ideVersion);
    CliArguments args = new CliArguments();
    // act
    context.run(args);
    // assert
    assertThat(context).log().hasNoMessageContaining("However, this is too old as your project requires at latest version");
  }

  @Test
  public void testRunWithoutLogging() {

    // arrange
    String testWarningMessage = "Test warning message";
    String testWarningMessage2 = "Other warning message";
    String testWarningMessage3 = "Final warning message";
    String testInfoMessage = "Test info message";
    String testDebugMessage = "Test debug message that will be suppressed because of threshold";
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    // act
    context.warning(testWarningMessage);
    // assert
    assertThat(context).logAtWarning().hasMessage(testWarningMessage);
    // and act
    context.runWithoutLogging(() -> {
      context.warning(testWarningMessage2);
      context.info(testInfoMessage);
      context.debug(testDebugMessage);
      assertThat(context).log().hasNoMessage(testWarningMessage2);
      assertThat(context).log().hasNoMessage(testInfoMessage);
      assertThat(context).log().hasNoMessage(testDebugMessage);
    }, IdeLogLevel.INFO);
    context.warning(testWarningMessage3);

    assertThat(context).log()
        .hasEntries(IdeLogEntry.ofWarning(testWarningMessage), IdeLogEntry.ofWarning(testWarningMessage2), IdeLogEntry.ofInfo(testInfoMessage),
            IdeLogEntry.ofWarning(testWarningMessage3));
    assertThat(context).log().hasNoMessage(testDebugMessage);
  }

}
