package com.devonfw.tools.ide.variable;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.VariableLine;

/**
 * Interface for a definition of a variable.
 *
 * @param <V> the {@link #getValueType() value type}.
 */
public interface VariableDefinition<V> {

  /**
   * @return the name of the variable.
   */
  String getName();

  /**
   * @return the optional legacy name that is still supported for downward compatibility. May be {@code null} if
   *         undefined (no legacy support).
   */
  String getLegacyName();

  /**
   * @return the {@link Class} reflecting the type of the variable value.
   */
  Class<V> getValueType();

  /**
   * @param context the {@link IdeContext}.
   * @return the default value. May be {@code null}.
   */
  V getDefaultValue(IdeContext context);

  /**
   * @param context the {@link IdeContext}.
   * @return the default value as {@link String}. May be {@code null}.
   *
   * @see #getDefaultValue(IdeContext)
   * @see #toString(Object)
   */
  default String getDefaultValueAsString(IdeContext context) {

    V value = getDefaultValue(context);
    if (value == null) {
      return null;
    }
    return toString(value);
  }

  /**
   * @return {@code true} if the {@link #getDefaultValue(IdeContext) default value} shall be used without any
   *         {@link EnvironmentVariables#get(String) variable lookup} (to prevent odd overriding of build in variables
   *         like IDE_HOME), {@code false} otherwise (overriding of default value is allowed and intended).
   */
  boolean isForceDefaultValue();

  /**
   * @param value the value as {@link String}. May NOT be {@code null}.
   * @param context TODO
   * @return the value converted to the {@link #getValueType() value type}.
   */
  V fromString(String value, IdeContext context);

  /**
   * @param value the typed value.
   * @return the value converted to {@link String}.
   */
  default String toString(V value) {

    if (value == null) {
      return "";
    }
    return value.toString();
  }

  /**
   * @param context the {@link IdeContext}.
   * @return the value of the variable of this {@link VariableDefinition}.
   */
  V get(IdeContext context);

  /**
   * @param line the {@link VariableLine} that potentially needs to be migrated.
   * @return the original {@link VariableLine} or a migrated copy of it.
   */
  default VariableLine migrateLine(VariableLine line) {

    String name = line.getName();
    if (name != null) {
      String newName = getName();
      if (!name.equals(newName)) {
        return line.withName(newName);
      }
    }
    return line;
  }

}
