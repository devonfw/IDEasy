package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Test of {@link EnvironmentVariables}.
 */
class EnvironmentVariablesTest extends AbstractIdeContextTest {

  private static final String ENVIRONMENT_PROJECT = "environment";

  /**
   * Test of {@link EnvironmentVariables#resolve(String, Object)} with self referencing variables.
   */
  @Test
  void testProperEvaluationOfVariables() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String TEST_ARGS1 = variables.get("TEST_ARGS1");
    String TEST_ARGS2 = variables.get("TEST_ARGS2");
    String TEST_ARGS3 = variables.get("TEST_ARGS3");
    String TEST_ARGS4 = variables.get("TEST_ARGS4");
    String TEST_ARGS5 = variables.get("TEST_ARGS5");
    String TEST_ARGS6 = variables.get("TEST_ARGS6");
    String TEST_ARGS7 = variables.get("TEST_ARGS7");
    String TEST_ARGS8 = variables.get("TEST_ARGS8");
    String TEST_ARGS9 = variables.get("TEST_ARGS9");
    String TEST_ARGS10 = variables.get("TEST_ARGS10");
    // some more advanced cases
    String TEST_ARGSa = variables.get("TEST_ARGSa");
    String TEST_ARGSb = variables.get("TEST_ARGSb");
    String TEST_ARGSc = variables.get("TEST_ARGSc");
    String TEST_ARGSd = variables.get("TEST_ARGSd");

    // assert
    assertThat(TEST_ARGS1).isEqualTo(" user1 settings1 workspace1 conf1");
    assertThat(TEST_ARGS2).isEqualTo(" user2 conf2");
    assertThat(TEST_ARGS3).isEqualTo(" user3 workspace3");
    assertThat(TEST_ARGS4).isEqualTo(" settings4");
    assertThat(TEST_ARGS5).isEqualTo(" settings5 conf5");
    assertThat(TEST_ARGS6).isEqualTo(" settings6 workspace6 conf6");

    assertThat(TEST_ARGS7).isEqualTo("user7 settings7 workspace7 conf7");
    assertThat(TEST_ARGS8).isEqualTo("settings8 workspace8 conf8");
    assertThat(TEST_ARGS9).isEqualTo("settings9 workspace9");
    assertThat(TEST_ARGS10).isEqualTo("user10 workspace10");

    assertThat(TEST_ARGSa).isEqualTo(" user1 settings1 workspace1 conf1  user3 workspace3 confa");
    assertThat(TEST_ARGSb).isEqualTo(
        "user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb");

    assertThat(TEST_ARGSc).isEqualTo(" user1 settings1 workspace1 conf1 userc settingsc confc");
    assertThat(TEST_ARGSd).isEqualTo(" user1 settings1 workspace1 conf1 userd workspaced");
  }

  /**
   * Test of {@link EnvironmentVariables#getToolVersionVariable(String)} and {@link EnvironmentVariables#getToolEditionVariable(String)}.
   */
  @Test
  void testGetToolVariable() {

    assertThat(EnvironmentVariables.getToolVersionVariable("android-studio")).isEqualTo("ANDROID_STUDIO_VERSION");
    assertThat(EnvironmentVariables.getToolEditionVariable("android-studio")).isEqualTo("ANDROID_STUDIO_EDITION");
  }

  /**
   * Test of {@link EnvironmentVariablesSystem} not inheriting specific environment variables leaking values from other projects into the current one.
   */
  @Test
  void testSpecificEnvironmentVariablesNotInheritedFromOtherProject() {

    // arrange
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, null, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String mavenArgs = IdeVariables.MAVEN_ARGS.get(context);
    Path m2Repo = IdeVariables.M2_REPO.get(context);
    String javaHome = variables.get("JAVA_HOME");
    String npmVersion = variables.get("NPM_VERSION");
    String otherVariable = variables.get("OTHER_VARIABLE");

    // assert
    assertThat(mavenArgs).isEqualTo("-s " + context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_FILE));
    assertThat(javaHome).isNotEqualTo("/usr/share/java");
    assertThat(npmVersion).isNull();
    assertThat(m2Repo).isEqualTo(context.getUserHome().resolve(Mvn.MVN_CONFIG_LEGACY_FOLDER).resolve("repository"));
    assertThat(otherVariable).isEqualTo("other value");
  }
}
