package com.devonfw.tools.ide.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link ExpressionParser}.
 * <p>
 * The expressions are resolved through the real {@link EnvironmentVariables#resolve(String, Object, boolean)} of an {@link IdeTestContext} (no mock of the
 * expression context) so that the behaviour of the surrounding variable resolution is exercised as well.
 */
class ExpressionParserTest extends AbstractIdeContextTest {

  /**
   * Test of {@code @path} with the default mode that replaces backslashes with slashes.
   */
  @Test
  void testPathUnixIsDefault() {

    // arrange (a plain variable is used since the built-in IDE_HOME is computed and cannot be overridden)
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("MY_BASE", "D:\\projects\\my-project");

    // act
    String result = context.getVariables().resolve("@path('$[MY_BASE]/software/mvn')", "test", false);

    // assert
    assertThat(result).isEqualTo("D:/projects/my-project/software/mvn");
  }

  /**
   * Test of {@code @path} with mode {@code native} on windows.
   */
  @Test
  void testPathNativeOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("MY_BASE", "D:\\projects\\my-project");

    // act
    String result = context.getVariables().resolve("@path('$[MY_BASE]/software/node/node.exe', native)", "test", false);

    // assert
    assertThat(result).isEqualTo("D:\\projects\\my-project\\software\\node\\node.exe");
  }

  /**
   * Test that {@code @path} with mode {@code native} converts an absolute MSYS path (git-bash) to the according windows drive instead of turning the drive
   * letter into a folder.
   */
  @Test
  void testPathNativeConvertsMsysPathOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);

    // act
    String result = context.getVariables().resolve("@path('/d/projects/my-project/software/mvn', native)", "test", false);

    // assert
    assertThat(result).isEqualTo("D:\\projects\\my-project\\software\\mvn");
  }

  /**
   * Test that {@code @path} with mode {@code native} still converts the separators of a relative path on windows.
   */
  @Test
  void testPathNativeConvertsRelativePathOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);

    // act
    String result = context.getVariables().resolve("@path('software/node/node.exe', native)", "test", false);

    // assert
    assertThat(result).isEqualTo("software\\node\\node.exe");
  }

  /**
   * Test that a backslash inside a quoted argument is never interpreted as an escape character, since arguments regularly contain native windows paths.
   */
  @Test
  void testBackslashIsNotAnEscapeCharacter() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act
    String result = context.getVariables().resolve("@path('C:\\Users\\login\\next')", "test", false);

    // assert
    assertThat(result).isEqualTo("C:/Users/login/next");
  }

  /**
   * Test that a quoted argument may contain the argument separator and the closing parenthesis. This is the reason why the argument list cannot be parsed with
   * a regular expression.
   */
  @Test
  void testQuotedArgumentMayContainCommaAndParenthesis() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("token-value");

    // act
    String result = context.getVariables().resolve("@ask-secret('AI_API_KEY', 'Enter your key (from the portal), please:')", "test", false);

    // assert
    assertThat(result).isEqualTo("token-value");
    assertThat(context).log()
        .hasEntries(new IdeLogEntry(IdeLogLevel.INTERACTION, "Enter your key (from the portal), please:", true));
  }

  /**
   * Test that a function may be nested inside the argument of another function.
   */
  @Test
  void testNestedFunction() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);

    // act
    String result = context.getVariables().resolve("@if-windows('@path(C:/a/b, native)')", "test", false);

    // assert
    assertThat(result).isEqualTo("C:\\a\\b");
  }

  /**
   * Test that an expression of a foreign syntax is passed through entirely untouched. IDEasy must never try to resolve placeholders that belong to another
   * tool.
   *
   * @param value the value that must not be modified.
   */
  @ParameterizedTest
  @ValueSource(strings = { //
      "@media (max-width: 600px) { a: 1 }", //
      "@media(max-width:600px){a:1}", //
      "@include button-variant($primary);", //
      "@Override @SuppressWarnings(\"unchecked\")", //
      "@param foo the foo", //
      "\"@angular/core\": \"^17.0.0\"", //
      "contact: dev@example.com", //
      "@path('unbalanced'" })
  void testForeignExpressionIsUntouched(String value) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act
    String result = context.getVariables().resolve(value, "test", false);

    // assert
    assertThat(result).isEqualTo(value);
  }

  /**
   * Test that an already defined variable is returned without asking the user.
   */
  @Test
  void testDefinedVariableIsNotAsked() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("AI_BACKEND_URL", "http://llama.local");

    // act
    String result = context.getVariables().resolve("@ask-variable('AI_BACKEND_URL')", "test", false);

    // assert
    assertThat(result).isEqualTo("http://llama.local");
    // no answers were provided, so an attempt to ask would have thrown an "End of answers reached!" error
    assertThat(context.getSecretLineCount()).isZero();
  }

  /**
   * Test that an undefined variable is asked with the default question and persisted to {@code conf/ide.properties} so that the user is only asked once.
   */
  @Test
  void testUndefinedVariableIsAskedAndPersisted() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("http://llama.local");
    EnvironmentVariables variables = context.getVariables();

    // act
    String result = variables.resolve("@ask-variable('AI_BACKEND_URL')", "test", false);

    // assert
    assertThat(result).isEqualTo("http://llama.local");
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Please enter the value for the variable AI_BACKEND_URL:", true));
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("AI_BACKEND_URL")).isEqualTo("http://llama.local");
  }

  /**
   * Test that an empty 1st argument always asks the user and never persists.
   */
  @Test
  void testEmptyVariableNameAlwaysAsks() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("first", "second");
    EnvironmentVariables variables = context.getVariables();

    // act
    String result = variables.resolve("@ask-variable('', 'Question A:')@ask-variable('', 'Question B:')", "test", false);

    // assert
    assertThat(result).isEqualTo("firstsecond");
    // nothing was persisted since there is no variable name to persist under
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("VAR_A")).isNull();
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("VAR_B")).isNull();
  }

  /**
   * Test that the 3rd argument allows an empty value to be entered. This is the intended way to permit an empty password in test or development scenarios.
   */
  @Test
  void testEmptyDefaultAllowsEmptyInput() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("");

    // act
    String result = context.getVariables().resolve("@ask-secret('OPTIONAL_PASSWORD', 'Password (may be empty):', conf, '')", "test", false);

    // assert
    assertThat(result).isEmpty();
  }

  /**
   * Test that in batch mode with force enabled a variable without a default value resolves to the empty string and is NOT persisted, so that the user is asked
   * again on the next interactive run.
   */
  @Test
  void testBatchModeWithForceDoesNotPersistMissingValue() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getStartContext().setBatchMode(true);
    context.getStartContext().setForceMode(true);
    EnvironmentVariables variables = context.getVariables();

    // act
    String result = variables.resolve("@ask-secret('MY_TOKEN')", "test", false);

    // assert
    assertThat(result).isEmpty();
    // a value that could not be asked is not persisted (and @ask-secret is never persisted anyway)
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("MY_TOKEN")).isNull();
  }

  /**
   * Test that in batch mode with force enabled an explicitly given default value is used and persisted.
   */
  @Test
  void testBatchModeWithForceUsesAndPersistsDefaultValue() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getStartContext().setBatchMode(true);
    context.getStartContext().setForceMode(true);
    EnvironmentVariables variables = context.getVariables();

    // act
    String result = variables.resolve("@ask-variable('MY_VARIABLE', 'Question:', conf, 'the-default')", "test", false);

    // assert
    assertThat(result).isEqualTo("the-default");
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("MY_VARIABLE")).isEqualTo("the-default");
  }

  /**
   * Test that the 3rd argument selects the configuration location the variable is persisted to.
   */
  @Test
  void testConfigLocationArgument() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("value-a", "value-b", "value-c");
    EnvironmentVariables variables = context.getVariables();

    // act
    variables.resolve("@ask-variable('VAR_SETTINGS', 'Q:', settings)", "test", false);
    variables.resolve("@ask-variable('VAR_HOME', 'Q:', home)", "test", false);
    variables.resolve("@ask-variable('VAR_DEFAULT', 'Q:')", "test", false);

    // assert
    assertThat(variables.getByType(EnvironmentVariablesType.SETTINGS).getFlat("VAR_SETTINGS")).isEqualTo("value-a");
    assertThat(variables.getByType(EnvironmentVariablesType.USER).getFlat("VAR_HOME")).isEqualTo("value-b");
    assertThat(variables.getByType(EnvironmentVariablesType.CONF).getFlat("VAR_DEFAULT")).isEqualTo("value-c");
  }

  /**
   * Test that an invalid configuration location is rejected.
   */
  @Test
  void testInvalidConfigLocation() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act + assert
    assertThatThrownBy(() -> context.getVariables().resolve("@ask-variable('MY_VARIABLE', 'Q:', somewhere)", "test", false))
        .isInstanceOf(CliException.class).hasMessageContaining("invalid configuration location 'somewhere'");
  }

  /**
   * Test that the default value is appended to the question in square brackets.
   */
  @Test
  void testDefaultValueIsAppendedToQuestion() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("");

    // act
    String result = context.getVariables().resolve("@ask-variable('MY_VARIABLE', 'Please enter the value:', conf, 'the-default')", "test", false);

    // assert
    assertThat(result).isEqualTo("the-default");
    assertThat(context).log()
        .hasEntries(new IdeLogEntry(IdeLogLevel.INTERACTION, "Please enter the value: [the-default]", true));
  }

  /**
   * Test that an explicit {@code null} as 4th argument means there is no default value and no suffix is appended.
   */
  @Test
  void testExplicitNullMeansNoDefaultValue() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("typed-value");

    // act
    String result = context.getVariables().resolve("@ask-variable('MY_VARIABLE', 'Please enter the value:', conf, null)", "test", false);

    // assert
    assertThat(result).isEqualTo("typed-value");
    assertThat(context).log()
        .hasEntries(new IdeLogEntry(IdeLogLevel.INTERACTION, "Please enter the value:", true));
  }

  /**
   * Test that an empty 1st argument without an explicit question is rejected.
   */
  @Test
  void testEmptyVariableNameRequiresQuestion() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act + assert
    assertThatThrownBy(() -> context.getVariables().resolve("@ask-variable('')", "test", false)).isInstanceOf(CliException.class)
        .hasMessageContaining("requires an explicit question");
  }

  /**
   * Test that an invalid number of arguments is rejected.
   */
  @Test
  void testInvalidArgumentCount() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act + assert
    assertThatThrownBy(() -> context.getVariables().resolve("@path(a, unix, extra)", "test", false)).isInstanceOf(CliException.class)
        .hasMessageContaining("requires 1 to 2 argument(s) but received 3");
  }

  /**
   * Test that an invalid mode for {@code @path} is rejected with a {@link CliException} so that a template authoring error is not reported as an internal error
   * of IDEasy.
   */
  @Test
  void testInvalidPathMode() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act + assert
    assertThatThrownBy(() -> context.getVariables().resolve("@path('x', dos)", "test", false)).isInstanceOf(CliException.class)
        .hasMessageContaining("invalid mode 'dos'");
  }

  /**
   * Test that {@code @ask-secret} uses the masked input path and {@code @ask-variable} does not.
   */
  @Test
  void testSecretUsesMaskedInputPath() {

    // arrange
    IdeTestContext secretContext = newContext(PROJECT_BASIC);
    secretContext.setAnswers("dummy-secret-value");
    IdeTestContext plainContext = newContext(PROJECT_BASIC);
    plainContext.setAnswers("http://llama.local");

    // act
    secretContext.getVariables().resolve("@ask-secret('MY_TOKEN')", "test", false);
    plainContext.getVariables().resolve("@ask-variable('MY_URL')", "test", false);

    // assert
    assertThat(secretContext.getSecretLineCount()).isEqualTo(1);
    assertThat(plainContext.getSecretLineCount()).isZero();
    assertThat(secretContext).log().hasNoMessageContaining("dummy-secret-value");
  }

}
