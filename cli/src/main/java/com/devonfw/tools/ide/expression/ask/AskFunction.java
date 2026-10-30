package com.devonfw.tools.ide.expression.ask;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.expression.AbstractExpressionFunction;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link ExpressionFunction} {@code @ask-variable} that asks for a variable in plain text and {@code @ask-secret} that asks for a secret variable with masked
 * input.
 * <ol>
 * <li>the name of the requested variable. If the variable is already defined it is returned without asking. If the
 * empty string is given, the user is always asked.</li>
 * <li>optional: an explicit question used as prompt. If omitted, defaults to
 * {@code Please enter the value for the (secret) variable «NAME»:}. If the 1st argument is empty, this argument is
 * required.</li>
 * <li>optional: the configuration location to persist the variable to, analogous to the {@code --cfg} option:
 * {@code settings}, {@code workspace}, {@code conf} or {@code home} ({@code user}). Defaults to {@code conf}.
 * Only applies to {@code @ask-variable}; a value entered for {@code @ask-secret} is never persisted (see below).</li>
 * <li>optional: a default value. It is appended to the question in square brackets so the user can just hit return.
 * Provide the empty string ({@code ''}) to allow empty input. If omitted or given as {@code null}, empty input is not
 * allowed and the user is asked again.</li>
 * </ol>
 * Example: {@code @ask-secret('AI_API_KEY', 'Please enter your API key:')}
 * <p>
 * <b>Note:</b> a value entered for {@code @ask-secret} is masked while typing. It is
 * <em>not persisted</em>, so the user is asked again on every run; persisting the plain text to an {@code ide.properties}
 * would just cache the secret on disk. Encryption is a separate story and out of scope here.
 */
public abstract class AskFunction extends AbstractExpressionFunction {

  private final StringProperty variableName;

  private final StringProperty question;

  private final LocationProperty location;

  private final StringProperty defaultValue;

  protected AskFunction() {

    super();
    this.variableName = add(new StringProperty("variableName", true));
    this.question = add(new StringProperty("question", false));
    this.location = add(new LocationProperty());
    this.defaultValue = add(new StringProperty("defaultValue", false));
  }

  /**
   * @return {@code true} for secret variable value (e.g. password), {@code false} otherwise.
   */
  protected boolean isSecret() {

    return false;
  }

  @Override
  protected String execute(ExpressionContext context) {

    String varName = this.variableName.getValue();
    if (!varName.isEmpty()) {
      String value = context.getVariable(varName);
      if (value != null) {
        return value;
      }
    }
    String result = ask(context);
    if (!isSecret() && !result.isEmpty()) {
      // secret values are deliberately NOT persisted so that the secret is not cached on disk (as plain text) in the ide.properties;
      // the user is asked again on every run.
      EnvironmentVariablesType locationArg = this.location.getValueOrDefault(EnvironmentVariablesType.CONF);
      context.setVariable(varName, result, locationArg);
    }
    return result;
  }

  /**
   * @return the value entered by the user, the default value, or {@code null} if the user could not be asked (batch mode with force) and no default value was
   *     given.
   */
  private String ask(ExpressionContext context) {
    boolean secret = isSecret();
    String varName = this.variableName.getValue();
    String prompt = this.question.getValue();
    if ((prompt == null) || prompt.isEmpty()) {
      if (varName.isEmpty()) {
        throw throwParameterMismatch("An explicit question is required as 2nd argument if the variable name is empty.");
        // the user is always asked and nothing is persisted since we have no name to persist under
      } else {
        prompt = "Please enter the value for the " + (secret ? "secret " : "") + "variable " + varName + ":";
      }
    }
    String fallback = this.defaultValue.getValue();
    if (fallback != null) {
      // show the default so the user can just hit return
      prompt = prompt + " [" + fallback + "]";
    }
    IdeContext ideContext = context.getIdeContext();
    String result;
    if (secret) {
      result = ideContext.askForSecret(prompt, fallback);
    } else {
      result = ideContext.askForInput(prompt, fallback);
    }
    return toResult(result);
  }

  private static String toResult(String value) {

    return (value == null) ? "" : value;
  }

  private static class LocationProperty extends EnumProperty<EnvironmentVariablesType> {

    private LocationProperty() {
      super("location", false, EnvironmentVariablesType.class);
    }

    @Override
    public EnvironmentVariablesType parse(String valueAsString, IdeContext context) {

      if ("home".equals(valueAsString)) {
        // legacy compatibility from design flaw
        return EnvironmentVariablesType.USER;
      }
      return super.parse(valueAsString, context);
    }
  }

}
