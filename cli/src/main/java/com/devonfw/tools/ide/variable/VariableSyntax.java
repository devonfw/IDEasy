package com.devonfw.tools.ide.variable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum with the available syntax for variables.
 *
 * @see com.devonfw.tools.ide.environment.EnvironmentVariables#resolve(String, Object, boolean)
 */
public enum VariableSyntax {

  /**
   * Syntax using curly braces ("${...}"). Considered legacy in IDEasy and only supported for devonfw-ide backward compatibility.
   *
   * @see IdeVariables#IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED
   */
  CURLY("\\$\\{([a-zA-Z0-9_-]+)\\}") {
    @Override
    public String create(String variableName) {
      return "${" + variableName + "}";
    }
  },

  /**
   * Syntax using square brackets ("$[...]").
   */
  SQUARE("\\$\\[([a-zA-Z0-9_-]+)\\]") {
    @Override
    public String create(String variableName) {
      return "$[" + variableName + "]";
    }
  };

  private final Pattern pattern;

  VariableSyntax(String regex) {

    this.pattern = Pattern.compile(regex);
  }

  /**
   * @return the regular expression {@link Pattern} for this {@link VariableSyntax}.
   */
  public Pattern getPattern() {

    return this.pattern;
  }

  /**
   * @param matcher the current {@link Matcher}.
   * @return the variable name.
   */
  public String getVariable(Matcher matcher) {
    return matcher.group(1);
  }

  /**
   * @param variableName the variable name.
   * @return the variable syntax for the given {@code variableName}. E.g. for {@link #CURLY} and the given {@link String}
   * "JAVA_HOME" this method would return "${JAVA_HOME}".
   */
  public abstract String create(String variableName);

}
