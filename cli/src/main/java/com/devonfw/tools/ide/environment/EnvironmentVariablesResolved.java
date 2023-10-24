package com.devonfw.tools.ide.environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

/**
 * Implementation of {@link EnvironmentVariables} that resolves variables recursively.
 */
public class EnvironmentVariablesResolved extends AbstractEnvironmentVariables {

  // Variable surrounded with "${" and "}" such as "${JAVA_HOME}" 1......2........
  private static final Pattern VARIABLE_SYNTAX = Pattern.compile("(\\$\\{([^}]+)})");

  private static final int MAX_RECURSION = 9;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   */
  EnvironmentVariablesResolved(AbstractEnvironmentVariables parent) {

    super(parent, parent.context);
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.RESOLVED;
  }

  @Override
  public String getFlat(String name) {

    return null;
  }

  @Override
  public String get(String name) {

    String value = getValue(name);
    if (value != null) {
      value = resolve(value, name, 0, name, value);
    }
    return value;
  }

  private String getValue(String name) {

    VariableDefinition<?> var = IdeVariables.get(name);
    String value;
    if ((var != null) && var.isForceDefaultValue()) {
      value = var.getDefaultValueAsString(this.context);
    } else {
      value = this.parent.get(name);
    }
    if ((value == null) && (var != null)) {
      String key = var.getName();
      if (!name.equals(key)) {
        value = this.parent.get(key);
      }
      if (value != null) {
        value = var.getDefaultValueAsString(this.context);
      }
    }
    if ((value != null) && (value.startsWith("~/"))) {
      value = this.context.getUserHome() + value.substring(1);
    }
    return value;
  }

  String resolve(String value, String name, int recursion, String rootName, String rootValue) {

    if (value == null) {
      return null;
    }
    if (recursion > MAX_RECURSION) {
      throw new IllegalStateException("Reached maximum recursion resolving " + value + " for root valiable " + rootName
          + " with value '" + rootValue + "'.");
    }
    recursion++;
    Matcher matcher = VARIABLE_SYNTAX.matcher(value);
    if (!matcher.find()) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length() + 8);
    do {
      String variableName = matcher.group(2);
      String variableValue = getValue(variableName);
      if (variableValue == null) {
        this.context.warning("Undefined variable {} in '{}={}' for root '{}={}'", variableName, name, value, rootName,
            rootValue);
      } else {
        String replacement = resolve(variableValue, variableName, recursion, rootName, rootValue);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      }
    } while (matcher.find());
    matcher.appendTail(sb);
    String resolved = sb.toString();
    return resolved;
  }

  @Override
  public EnvironmentVariables resolved() {

    return this;
  }

}
