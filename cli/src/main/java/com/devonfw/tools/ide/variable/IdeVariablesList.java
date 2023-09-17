package com.devonfw.tools.ide.variable;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal helper class to compute {@link Map} of all predefined {@link VariableDefinition}s.
 */
final class IdeVariablesList {

  private static final Map<String, VariableDefinition<?>> VARIABLE_MAP;

  static {
    VARIABLE_MAP = new HashMap<>();
    for (VariableDefinition<?> var : IdeVariables.VARIABLES) {
      register(var, VARIABLE_MAP, false);
      register(var, VARIABLE_MAP, true);
    }
  }

  private static void register(VariableDefinition<?> var, Map<String, VariableDefinition<?>> map, boolean legacy) {

    String key;
    if (legacy) {
      key = var.getLegacyName();
      if (key == null) {
        return;
      }
    } else {
      key = var.getName();
    }
    VariableDefinition<?> duplicate = map.put(key, var);
    if (duplicate != null) {
      throw new IllegalStateException("Duplicate variables for key '" + key + "': " + var + " and " + duplicate);
    }
  }

  static VariableDefinition<?> get(String name) {

    return VARIABLE_MAP.get(name);
  }

}
