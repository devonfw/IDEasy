package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.variable.IdeVariables;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Integration test of {@link IdeContext}.
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
    assertLogMessage(context, IdeLogLevel.WARNING, "Undefined variable UNDEFINED in 'SOME=some-${UNDEFINED}' for root 'FOO=foo-${BAR}'");
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
    String envPath = System.getenv(IdeVariables.PATH.getName());
    envPath = envPath.replaceAll("[\\\\][;]", ";").replaceAll("[\\/][:]", ":");
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
