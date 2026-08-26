package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Test of {@link EnvironmentVariables}.
 */
class EnvironmentVariablesTest extends AbstractIdeContextTest {

  private static final String ENVIRONMENT_PROJECT = "environment";

  private static final String MAVEN_ARGS_MERGE_PROJECT = "mvn-args";

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
    assertThat(EnvironmentVariables.getToolExtraPluginsVariable("android-studio")).isEqualTo("ANDROID_STUDIO_EXTRA_PLUGINS");
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
    assertThat(m2Repo).isEqualTo(context.getUserHome().resolve(Mvn.MVN_CONFIG_LEGACY_FOLDER).resolve(IdeContext.FOLDER_REPOSITORY));
    assertThat(otherVariable).isEqualTo("other value");
  }

  /**
   * Test that {@link IdeVariables#MAVEN_ARGS} has the appendDefaultValue flag enabled, and that other variables do not.
   */
  @Test
  void testMavenArgsAppendDefaultValueFlagIsEnabled() {

    assertThat(IdeVariables.MAVEN_ARGS.isDefaultValueAppended()).isTrue();
    assertThat(IdeVariables.MVN_BUILD_OPTS.isDefaultValueAppended()).isFalse();
    assertThat(IdeVariables.IDE_HOME.isDefaultValueAppended()).isFalse();
  }

  /**
   * Test that a user-defined {@code MAVEN_ARGS} in {@code conf/ide.properties} is kept and IDEasy's defaults are appended to it, not replaced.
   */
  @Test
  void testUserDefinedMavenArgsIsMergedWithIdeasyDefaults() {

    // arrange
    IdeTestContext context = newContext(MAVEN_ARGS_MERGE_PROJECT, null, false);

    // act
    String mavenArgs = IdeVariables.MAVEN_ARGS.get(context);

    // assert
    Path settingsFile = context.getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_FILE);
    assertThat(mavenArgs).isEqualTo("-X -e -s " + settingsFile);
  }

  /**
   * Test that IDEasy's {@code -s} and {@code -Dsettings.security=} arguments override any user-provided ones and that unrelated user arguments are correctly
   * appended.
   */
  @Test
  void testMergeMavenArgsWithDefault() {

    String defaultValue = "-s /ide/settings.xml -Dsettings.security=/ide/settings-security.xml";

    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-Xmx8000m -s invalid/settings.xml -Dsettings.security=something_wrong", defaultValue))
        .isEqualTo("-Xmx8000m " + defaultValue);
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-Xmx8000m -s invalid/settings.xml", defaultValue))
        .isEqualTo("-Xmx8000m " + defaultValue);
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-Xmx8000m -Dsettings.security=something_wrong", defaultValue))
        .isEqualTo("-Xmx8000m " + defaultValue);
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-T 4", defaultValue))
        .isEqualTo("-T 4 " + defaultValue);
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-s", defaultValue))
        .isEqualTo(defaultValue);
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-Xmx8000m -s invalid/settings.xml", ""))
        .isEqualTo("-Xmx8000m -s invalid/settings.xml");
    assertThat(AbstractEnvironmentVariables.mergeWithDefault("-Xmx8000m -s invalid/settings.xml", null))
        .isEqualTo("-Xmx8000m -s invalid/settings.xml");
  }

  /**
   * Test of {@link EnvironmentVariables#resolve(String, Object)} with an {@code @ask-variable} expression for an undefined variable. The user is asked and the
   * entered value is persisted to {@code conf/ide.properties} so that the question is only asked once.
   */
  @Test
  void testResolveAskVariableExpressionPromptsAndPersists() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, true);
    context.setAnswers("http://llama.local");
    EnvironmentVariables variables = context.getVariables();

    // act
    String resolved = variables.resolve("url=@ask-variable('AI_BACKEND_URL')", "test", false);

    // assert
    assertThat(resolved).isEqualTo("url=http://llama.local");
    assertThat(context.getVariables().get("AI_BACKEND_URL")).isEqualTo("http://llama.local");
  }

  /**
   * Test that an {@code @ask-variable} expression for an already defined variable behaves exactly like a plain variable and does not interact with the user.
   */
  @Test
  void testResolveAskVariableExpressionUsesDefinedVariableWithoutInteraction() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String askExpression = variables.resolve("@ask-variable('TEST_ARGS4')", "test", false);
    String plainVariable = variables.resolve("$[TEST_ARGS4]", "test", false);

    // assert
    assertThat(askExpression).isEqualTo(plainVariable);
    assertThat(askExpression).endsWith(" settings4");
  }

  /**
   * Test of {@link EnvironmentVariables#resolve(String, Object)} with a {@code @path} expression whose argument contains a variable.
   */
  @Test
  void testResolvePathExpressionWithVariableArgument() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String resolved = variables.resolve("@path('$[IDE_HOME]/software/mvn')", "test", false);

    // assert
    assertThat(resolved).doesNotContain("\\\\");
    assertThat(resolved).endsWith("/software/mvn");
  }

  /**
   * Test that text which does not call a registered expression function is passed through untouched.
   */
  @Test
  void testResolveLeavesForeignExpressionUntouched() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String resolved = variables.resolve("@media(max-width:600px){a:1}", "test", false);

    // assert
    assertThat(resolved).isEqualTo("@media(max-width:600px){a:1}");
  }


  /**
   * Test that a value entered for {@code @ask-secret} is masked in all log output, in particular in the debug log written when it is persisted.
   */
  @Test
  void testEnteredSecretIsMaskedInLogOutput() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, true);
    context.setAnswers("dummy-secret-value");
    EnvironmentVariables variables = context.getVariables();

    // act
    String resolved = variables.resolve("token=@ask-secret('MY_TOKEN')", "test", false);

    // assert
    assertThat(resolved).isEqualTo("token=dummy-secret-value");
    assertThat(context).log().hasNoMessageContaining("dummy-secret-value");
  }

  /**
   * Test that an already defined secret variable is masked in log output as well, although the user is not asked for it. This is the case on every run after
   * the value has been persisted once.
   */
  @Test
  void testAlreadyDefinedSecretIsMaskedInLogOutput() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    // TRACE level so that the "Variable MY_TOKEN=..." log written while reading the variable is captured
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, true, null, IdeLogLevel.TRACE);
    EnvironmentVariables variables = context.getVariables();
    variables.getByType(EnvironmentVariablesType.CONF).set("MY_TOKEN", "dummy-stored-value");
    context.getTestStartContext().getEntries().clear();

    // act
    String resolved = variables.resolve("token=@ask-secret('MY_TOKEN')", "test", false);

    // assert
    assertThat(resolved).isEqualTo("token=dummy-stored-value");
    assertThat(context.getSecretLineCount()).isZero(); // the user was NOT asked
    assertThat(context).log().hasNoMessageContaining("dummy-stored-value");
  }

  /**
   * Test that a plain variable is still logged normally so that debugging is not impaired.
   */
  @Test
  void testPlainVariableIsNotMasked() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(ENVIRONMENT_PROJECT, path, true);
    context.setAnswers("http://llama.local");
    EnvironmentVariables variables = context.getVariables();

    // act
    String resolved = variables.resolve("url=@ask-variable('MY_URL')", "test", false);

    // assert
    assertThat(resolved).isEqualTo("url=http://llama.local");
    assertThat(context.getSecretLineCount()).isZero();
  }

}
