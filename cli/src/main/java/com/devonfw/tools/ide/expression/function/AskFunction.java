package com.devonfw.tools.ide.expression.function;

import java.util.List;
import java.util.Locale;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;

/**
 * {@link ExpressionFunction} {@code @ask-variable} that asks for a variable in plain text and {@code @ask-secret} that
 * asks for a secret variable with masked input.
 * <ol>
 * <li>the name of the requested variable. If the variable is already defined it is returned without asking. If the
 * empty string is given, the user is always asked.</li>
 * <li>optional: an explicit question used as prompt. If omitted, defaults to
 * {@code Please enter the value for the (secret) variable «NAME»:}. If the 1st argument is empty, this argument is
 * required.</li>
 * <li>optional: the configuration location to persist the variable to, analogous to the {@code --cfg} option:
 * {@code settings}, {@code workspace}, {@code conf} or {@code home} ({@code user}). Defaults to {@code conf}.</li>
 * <li>optional: a default value. It is appended to the question in angled brackets so the user can just hit return.
 * Provide the empty string ({@code ''}) to allow empty input. If omitted or given as {@code null}, empty input is not
 * allowed and the user is asked again.</li>
 * </ol>
 * Example: {@code @ask-secret('AI_API_KEY', 'Please enter your API key:', conf)}
 */
public class AskFunction implements ExpressionFunction {

  private static final String NAME_VARIABLE = "ask-variable";

  private static final String NAME_SECRET = "ask-secret";

  /** Literal value of the 4th argument meaning that there is no default value. */
  private static final String NULL_VALUE = "null";

  /** Alias for {@link EnvironmentVariablesFiles#USER} as the configuration location. */
  private static final String LOCATION_HOME = "HOME";

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

    return 4;
  }

  @Override
  public String apply(List<String> args, ExpressionContext context) {

    String variableName = args.get(0);
    String question = (args.size() > 1) ? args.get(1) : null;
    EnvironmentVariablesType location = toLocation((args.size() > 2) ? args.get(2) : null);
    String defaultValue = (args.size() > 3) ? args.get(3) : null;
    if (NULL_VALUE.equals(defaultValue)) {
      // an explicit "null" means no default value, so empty input is not allowed
      defaultValue = null;
    }

    if (variableName.isEmpty()) {
      if ((question == null) || question.isEmpty()) {
        throw new IllegalArgumentException(
            "Function @" + this.name + " requires an explicit question as 2nd argument if the variable name is empty.");
      }
      // the user is always asked and nothing is persisted since we have no name to persist under
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
      // In batch mode with force enabled the user cannot be asked and no default value was given. The expression
      // resolves to the empty string but is NOT persisted: otherwise the variable would be defined as empty forever
      // and the user would never be asked again on the next interactive run.
      return "";
    }
    context.setVariable(variableName, value, location);
    return value;
  }

  /**
   * @param arg the 3rd argument or {@code null} if omitted.
   * @return the {@link EnvironmentVariablesType} to persist to.
   */
  private EnvironmentVariablesType toLocation(String arg) {

    if ((arg == null) || arg.isEmpty()) {
      return EnvironmentVariablesType.CONF;
    }
    String location = arg.trim().toUpperCase(Locale.ROOT);
    if (LOCATION_HOME.equals(location)) {
      location = EnvironmentVariablesFiles.USER.name();
    }
    try {
      return EnvironmentVariablesFiles.valueOf(location).toType();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid configuration location '" + arg + "' for function @" + this.name
          + " - expected one of settings, workspace, conf or home.", e);
    }
  }

  private static String toResult(String value) {

    return (value == null) ? "" : value;
  }

  /**
   * @return the value entered by the user, the default value, or {@code null} if the user could not be asked (batch mode with force) and no default value was
   *     given.
   */
  private String ask(String question, String defaultValue, ExpressionContext context) {

    String prompt = question;
    if (defaultValue != null) {
      // show the default so the user can just hit return
      prompt = question + " <" + defaultValue + ">";
    }
    IdeContext ideContext = context.getIdeContext();
    if (this.secret) {
      return ideContext.askForSecret(prompt, defaultValue);
    }
    return ideContext.askForInput(prompt, defaultValue);
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
