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
   * Syntax using square brackets ("$[...]"). Additionally to plain variables ("$[MY_VARIABLE]") this syntax supports the function expression
   * "$[ask:MY_VARIABLE]" or "$[secret:MY_VARIABLE]" that will interactively ask the user for the value if the variable is undefined.
   */
  SQUARE("\\$\\[(?:([a-zA-Z0-9_-]+)|(ask|secret):([a-zA-Z0-9_-]+))\\]") {
    @Override
    public String create(String variableName) {
      return "$[" + variableName + "]";
    }

    @Override
    public String getAskVariable(Matcher matcher) {
      return matcher.group(3);
    }

    @Override
    public boolean isSecret(Matcher matcher) {
      return "secret".equals(matcher.group(2));
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
   * @param matcher the current {@link Matcher}.
   * @return the name of the variable to interactively ask the user for in case the current match is an ask expression (e.g. "$[ask:MY_VARIABLE]") or
   *     {@code null} if the current match is a plain {@link #getVariable(Matcher) variable}.
   */
  public String getAskVariable(Matcher matcher) {
    return null;
  }

  /**
   * @param matcher the current {@link Matcher}.
   * @return {@code true} if the current match is an {@link #getAskVariable(Matcher) ask expression} for a secret value (e.g. "$[secret:MY_TOKEN]")
   *     that shall not be echoed while typing, {@code false} otherwise.
   */
  public boolean isSecret(Matcher matcher) {
    return false;
  }

  /**
   * @param variableName the variable name.
   * @return the variable syntax for the given {@code variableName}. E.g. for {@link #CURLY} and the given {@link String} "JAVA_HOME" this method would return
   * "${JAVA_HOME}".
   */
  public abstract String create(String variableName);

}
