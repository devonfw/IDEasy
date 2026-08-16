package com.devonfw.tools.ide.expression.function;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;

/**
 * {@link ExpressionFunction} {@code @ask-variable} that asks for a variable in plain text and {@code @ask-secret} that asks for a secret variable with masked
 * input.
 * <ol>
 * <li>the name of the requested variable. If the variable is already defined it is returned without asking. If the
 * empty string is given, the user is always asked.</li>
 * <li>optional: an explicit question used as prompt. If omitted, defaults to
 * {@code Please enter the value for the (secret) variable «NAME»:}. If the 1st argument is empty, this argument is
 * required.</li>
 * <li>optional: a default value. Provide the empty string ({@code ''}) to allow empty input.</li>
 * </ol>
 * Example: {@code @ask-secret('AI_API_KEY', 'Please enter your API key for the AI backend:')}
 */
public class AskFunction implements ExpressionFunction {

  private static final String NAME_VARIABLE = "ask-variable";

  private static final String NAME_SECRET = "ask-secret";

  private final String name;

  private final boolean secret;

  private AskFunction(String name, boolean secret) {

    super();
    this.name = name;
    this.secret = secret;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public int getMinArgs() {

    return 1;
  }

  @Override
  public int getMaxArgs() {

    return 3;
  }

  @Override
  public String apply(List<String> args, ExpressionContext context) {

    String variableName = args.get(0);
    String question = (args.size() > 1) ? args.get(1) : null;
    String defaultValue = (args.size() > 2) ? args.get(2) : null;

    if (variableName.isEmpty()) {
      if ((question == null) || question.isEmpty()) {
        throw new IllegalArgumentException(
            "Function @" + this.name + " requires an explicit question as 2nd argument if the variable name is empty.");
      }
      return toResult(ask(question, defaultValue, context));
    }
    String value = context.getVariable(variableName);
    if (value != null) {
      return value;
    }
    if (question == null) {
      question = "Please enter the value for the " + (this.secret ? "secret " : "") + "variable " + variableName + ":";
    }
    value = ask(question, defaultValue, context);
    if (value == null) {
      return "";
    }
    if (context.isPersistent()) {
      context.setVariable(variableName, value);
    }
    return value;
  }

  private static String toResult(String value) {

    return (value == null) ? "" : value;
  }

  /**
   * @return the value entered by the user, the default value, or {@code null} if the user could not be asked (batch mode with force) and no default value was
   *     given.
   */
  private String ask(String question, String defaultValue, ExpressionContext context) {

    IdeContext ideContext = context.getIdeContext();
    if (this.secret) {
      return ideContext.askForSecret(question, defaultValue);
    }
    return ideContext.askForInput(question, defaultValue);
  }

  /**
   * @return the {@link AskFunction} for {@code @ask-variable}.
   */
  public static AskFunction ofVariable() {

    return new AskFunction(NAME_VARIABLE, false);
  }

  /**
   * @return the {@link AskFunction} for {@code @ask-secret}.
   */
  public static AskFunction ofSecret() {

    return new AskFunction(NAME_SECRET, true);
  }

}
