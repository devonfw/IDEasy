package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link IdeContext}.
 */
class IdeContextTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(IdeContextTest.class);

  /**
   * Test of {@link IdeContext} initialization from basic project.
   */
  @Test
  void testBasicProjectEnvironment() {

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
  void testWorkspacePathFallsBackToMainWorkspace() {
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
  void testProjectPathFallsBackToMainWorkspace() {
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
  void testCurrentWorkspacePathIsUsed() {
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
  void testIdeVersionTooSmall() {
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
  void testIdeVersionOk() {
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
  void testRunWithoutLogging() {

    // arrange
    String testWarningMessage = "Test warning message";
    String testWarningMessage2 = "Other warning message";
    String testWarningMessage3 = "Final warning message";
    String testInfoMessage = "Test info message";
    String testDebugMessage = "Test debug message that will be suppressed because of threshold";
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    // act
    LOG.warn(testWarningMessage);
    // assert
    assertThat(context).logAtWarning().hasMessage(testWarningMessage);
    // and act
    context.runWithoutLogging(() -> {
      LOG.warn(testWarningMessage2);
      LOG.info(testInfoMessage);
      LOG.debug(testDebugMessage);
      assertThat(context).log().hasNoMessage(testWarningMessage2);
      assertThat(context).log().hasNoMessage(testInfoMessage);
      assertThat(context).log().hasNoMessage(testDebugMessage);
    }, IdeLogLevel.INFO);
    LOG.warn(testWarningMessage3);

    assertThat(context).log()
        .hasEntries(IdeLogEntry.ofWarning(testWarningMessage), IdeLogEntry.ofWarning(testWarningMessage2), IdeLogEntry.ofInfo(testInfoMessage),
            IdeLogEntry.ofWarning(testWarningMessage3));
    assertThat(context).log().hasNoMessage(testDebugMessage);
  }

  /**
   * Tests if the BASH_PATH variable was set and the target is existing, the variable is returned by {@code findBash} and the debug message is correct (uses
   * environment.properties for BASH_PATH and PATH variables).
   */
  @Test
  void testFindBashOnBashPathOnWindows() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-bash-git", path, true);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    // act
    Path bash = context.findBash();

    // assert
    Path bashPath = context.getUserHome().resolve("PortableGit").resolve("bin").resolve("bash.exe");
    assertThat(context).logAtDebug().hasMessage("BASH_PATH environment variable was found and points to: " + bashPath);

    assertThat(bash).isEqualTo(
        bashPath);
  }

  /**
   * Tests if the BASH_PATH variable was set and the target is not existing and the error message is correct.
   */
  @Test
  void testFindBashOnBashPathOnWindowsWithoutExistingFileReturnsProperErrorMessage() {
    // arrange
    // create first context to prepare test data
    String path = "project/workspaces";
    IdeTestContext supportContext = newContext("find-bash-git", path, true);
    FileAccess fileAccess = supportContext.getFileAccess();
    Path environmentFile = supportContext.getUserHome().resolve("environment.properties");
    fileAccess.touch(environmentFile);
    Properties properties = fileAccess.readProperties(environmentFile);
    String notExisting = supportContext.getIdeHome().resolve("notexisting").toAbsolutePath().toString();
    properties.put("PATH", notExisting);
    properties.put("BASH_PATH", notExisting);
    fileAccess.writeProperties(properties, environmentFile);
    // create 2nd context using the modified test project
    IdeTestContext context = new IdeTestContext(supportContext.getIdeHome(), IdeLogLevel.TRACE, null);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    // act
    Path bash = context.findBash();

    // assert
    assertThat(context).logAtError()
        .hasMessage("The environment variable BASH_PATH points to a non existing file: " + notExisting);

    assertThat(bash).isNull();
  }

  /**
   * Tests if the BASH_PATH variable was set and the target is not existing and bash executable was found on the system PATH.
   */
  @Test
  void testFindBashOnSystemPathOnWindowsWithInvalidBashPathSet() {
    // arrange
    // create first context to prepare test data
    String path = "project/workspaces";
    IdeTestContext supportContext = newContext("find-bash-git", path, true);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    supportContext.setSystemInfo(systemInfo);
    FileAccess fileAccess = supportContext.getFileAccess();
    Path environmentFile = supportContext.getUserHome().resolve("environment.properties");
    fileAccess.touch(environmentFile);
    Properties properties = fileAccess.readProperties(environmentFile);
    Path gitPath = supportContext.getUserHome().resolve("PortableGit").resolve("bin").toAbsolutePath();
    Path bashExePath = gitPath.resolve("bash.exe");
    String notExisting = supportContext.getUserHome().resolve("notexisting").toAbsolutePath().toString();
    properties.put("PATH", gitPath + ":" + supportContext.getUserHome().resolve("AppData/Local/Microsoft/WindowsApps"));
    properties.put("BASH_PATH", notExisting);
    fileAccess.writeProperties(properties, environmentFile);
    // create 2nd context using the modified test project
    IdeTestContext context = new IdeTestContext(supportContext.getIdeHome(), IdeLogLevel.TRACE, null);
    context.setSystemInfo(systemInfo);
    // act
    Path bash = context.findBash();

    // assert
    assertThat(context).logAtDebug()
        .hasMessage("A proper bash executable was found in your PATH environment variable at: " + bashExePath);

    assertThat(bash).isEqualTo(bashExePath);
  }

}
