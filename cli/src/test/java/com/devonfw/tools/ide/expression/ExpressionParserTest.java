package com.devonfw.tools.ide.expression;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link ExpressionParser}.
 */
class ExpressionParserTest extends AbstractIdeContextTest {

  /**
   * Test of {@code @path} with the default mode that replaces backslashes with slashes.
   */
  @Test
  void testPathUnixIsDefault() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestExpressionContext expressionContext = new TestExpressionContext(context);
    expressionContext.variables.put("IDE_HOME", "D:\\projects\\my-project");

    // act
    String result = expressionContext.resolve("@path('$[IDE_HOME]/software/mvn')");

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
    TestExpressionContext expressionContext = new TestExpressionContext(context);
    expressionContext.variables.put("IDE_HOME", "D:\\projects\\my-project");

    // act
    String result = expressionContext.resolve("@path('$[IDE_HOME]/software/node/node.exe', native)");

    // assert
    assertThat(result).isEqualTo("D:\\projects\\my-project\\software\\node\\node.exe");
  }

  /**
   * Test that a backslash inside a quoted argument is never interpreted as an escape character, since arguments
   * regularly contain native windows paths.
   */
  @Test
  void testBackslashIsNotAnEscapeCharacter() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@path('C:\\Users\\login\\next')");

    // assert
    assertThat(result).isEqualTo("C:/Users/login/next");
  }

  /**
   * Test that a quoted argument may contain the argument separator and the closing parenthesis. This is the reason why
   * the argument list cannot be parsed with a regular expression.
   */
  @Test
  void testQuotedArgumentMayContainCommaAndParenthesis() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("token-value");
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-secret('AI_API_KEY', 'Enter your key (from the portal), please:')");

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
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@if-windows('@path(C:/a/b, native)')");

    // assert
    assertThat(result).isEqualTo("C:\\a\\b");
  }

  /**
   * Test that an expression of a foreign syntax is passed through entirely untouched. IDEasy must never try to resolve
   * placeholders that belong to another tool.
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
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve(value);

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
    TestExpressionContext expressionContext = new TestExpressionContext(context);
    expressionContext.variables.put("AI_BACKEND_URL", "http://llama.local");

    // act
    String result = expressionContext.resolve("@ask-variable('AI_BACKEND_URL')");

    // assert
    assertThat(result).isEqualTo("http://llama.local");
    assertThat(expressionContext.persisted).isEmpty();
  }

  /**
   * Test that an undefined variable is asked with the default question and persisted for workspace templates.
   */
  @Test
  void testUndefinedVariableIsAskedAndPersisted() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("http://llama.local");
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-variable('AI_BACKEND_URL')");

    // assert
    assertThat(result).isEqualTo("http://llama.local");
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Please enter the value for the variable AI_BACKEND_URL:", true));
    assertThat(expressionContext.persisted).containsExactly(Map.entry("AI_BACKEND_URL", "http://llama.local"));
  }

  /**
   * Test that a settings template does not persist the entered value since it is only instantiated once.
   */
  @Test
  void testSettingsTemplateDoesNotPersist() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("value");
    TestExpressionContext expressionContext = new TestExpressionContext(context);
    expressionContext.persistent = false;

    // act
    String result = expressionContext.resolve("@ask-variable('MY_VARIABLE')");

    // assert
    assertThat(result).isEqualTo("value");
    assertThat(expressionContext.persisted).isEmpty();
  }

  /**
   * Test that an empty 1st argument always asks the user and never persists.
   */
  @Test
  void testEmptyVariableNameAlwaysAsks() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("first", "second");
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-variable('', 'Question A:')@ask-variable('', 'Question B:')");

    // assert
    assertThat(result).isEqualTo("firstsecond");
    assertThat(expressionContext.persisted).isEmpty();
  }

  /**
   * Test that the 3rd argument allows an empty value to be entered. This is the intended way to permit an empty
   * password in test or development scenarios.
   */
  @Test
  void testEmptyDefaultAllowsEmptyInput() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("");
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-secret('OPTIONAL_PASSWORD', 'Password (may be empty):', '')");

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
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-secret('MY_TOKEN')");

    // assert
    assertThat(result).isEmpty();
    assertThat(expressionContext.persisted).isEmpty();
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
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act
    String result = expressionContext.resolve("@ask-variable('MY_VARIABLE', 'Question:', 'the-default')");

    // assert
    assertThat(result).isEqualTo("the-default");
    assertThat(expressionContext.persisted).containsExactly(Map.entry("MY_VARIABLE", "the-default"));
  }

  /**
   * Test that an empty 1st argument without an explicit question is rejected.
   */
  @Test
  void testEmptyVariableNameRequiresQuestion() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act + assert
    assertThatThrownBy(() -> expressionContext.resolve("@ask-variable('')")).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires an explicit question");
  }

  /**
   * Test that an invalid number of arguments is rejected.
   */
  @Test
  void testInvalidArgumentCount() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestExpressionContext expressionContext = new TestExpressionContext(context);

    // act + assert
    assertThatThrownBy(() -> expressionContext.resolve("@path(a, unix, extra)"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requires 1 to 2 argument(s) but received 3");
  }

  /**
   * Simple {@link ExpressionContext} for testing that also simulates the surrounding variable resolution of
   * {@code AbstractEnvironmentVariables}.
   */
  private static class TestExpressionContext implements ExpressionContext {

    private static final Pattern SQUARE = Pattern.compile("\\$\\[([a-zA-Z0-9_-]+)\\]");

    private final ExpressionParser parser = new ExpressionParser(ExpressionFunctionManager.get());

    private final Map<String, String> variables = new HashMap<>();

    private final Map<String, String> persisted = new LinkedHashMap<>();

    private final IdeContext ideContext;

    private boolean persistent = true;

    private TestExpressionContext(IdeContext ideContext) {

      super();
      this.ideContext = ideContext;
    }

    @Override
    public String resolve(String value) {

      String result = this.parser.resolve(value, this);
      Matcher matcher = SQUARE.matcher(result);
      StringBuilder sb = new StringBuilder();
      while (matcher.find()) {
        String variableValue = this.variables.get(matcher.group(1));
        matcher.appendReplacement(sb, Matcher.quoteReplacement(variableValue == null ? matcher.group() : variableValue));
      }
      matcher.appendTail(sb);
      return sb.toString();
    }

    @Override
    public IdeContext getIdeContext() {

      return this.ideContext;
    }

    @Override
    public String getVariable(String name) {

      return this.variables.get(name);
    }

    @Override
    public void setVariable(String name, String value) {

      this.persisted.put(name, value);
      this.variables.put(name, value);
    }

    @Override
    public boolean isPersistent() {

      return this.persistent;
    }
  }
}
