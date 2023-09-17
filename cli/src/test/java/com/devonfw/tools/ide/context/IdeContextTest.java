package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Integration test of {@link IdeContext}.
 */
@SuppressWarnings("javadoc")
public class IdeContextTest extends Assertions {

  public static final Path PATH_PROJECTS = Paths.get("src/test/resources/ide-projects");

  private IdeContext newContext(String path) {

    Path userDir = PATH_PROJECTS.resolve(path);
    return new IdeTestContext(userDir);
  }

  protected void assertLogMessage(IdeContext context, IdeLogLevel level, String message) {

    assertLogMessage(context, level, message, false);
  }

  protected void assertLogMessage(IdeContext context, IdeLogLevel level, String message, boolean contains) {

    IdeTestLogger logger = (IdeTestLogger) context.level(IdeLogLevel.WARNING);
    ListAssert<String> assertion = assertThat(logger.getMessages()).as(level.name() + "-Log messages");
    if (contains) {
      Condition<String> condition = new Condition<>() {
        public boolean matches(String e) {

          return e.contains(message);
        };
      };
      assertion.filteredOn(condition).isNotEmpty();
    } else {
      assertion.contains(message);
    }
  }

  /**
   * Test of {@link IdeContext} initialization from basic project.
   */
  @Test
  public void testBasicProjectEnvironment() {

    // arrange
    String path = "basic/workspaces/foo-test/my-git-repo";
    // act
    IdeContext context = newContext(path);
    // assert
    assertThat(context.getWorkspaceName()).isEqualTo("foo-test");
    assertThat(IdeVariables.DOCKER_EDITION.get(context)).isEqualTo("docker");
    EnvironmentVariables variables = context.getVariables();
    assertThat(variables.get("FOO")).isEqualTo("foo-bar-some-${UNDEFINED}");
    assertLogMessage(context, IdeLogLevel.WARNING,
        "Undefined variable UNDEFINED in 'SOME=some-${UNDEFINED}' for root 'FOO=foo-${BAR}'");
    assertThat(context.getIdeHome().resolve("readme")).hasContent("this is the IDE_HOME directory");
    assertThat(context.getIdeRoot().resolve("readme")).hasContent("this is the IDE_ROOT directory");
    assertThat(context.getUserHome().resolve("readme")).hasContent("this is the users HOME directory");
    assertThat(variables.getPath("M2_REPO")).isEqualTo(context.getUserHome().resolve(".m2/repository"));
    assertThat(context.getDownloadPath().resolve("readme")).hasContent("this is the download cache");
    assertThat(context.getUrlsPath().resolve("readme")).hasContent("this is the download metadata");
    assertThat(context.getToolRepository().resolve("readme")).hasContent("this is the tool repository");
    assertThat(context.getWorkspacePath().resolve("readme")).hasContent("this is the foo-test workspace of basic");
    SystemPath systemPath = IdeVariables.PATH.get(context);
    assertThat(systemPath).isSameAs(context.getPath());
    String envPath = System.getenv(IdeVariables.PATH.getName());
    assertThat(systemPath.toString()).isNotEqualTo(envPath).endsWith(envPath);
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

}
