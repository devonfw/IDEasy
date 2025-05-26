package com.devonfw.tools.ide.environment;

import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

/**
 * Implementation of {@link EnvironmentVariables} using {@link System#getenv()}.
 *
 * @see EnvironmentVariablesType#SYSTEM
 */
public final class EnvironmentVariablesSystem extends EnvironmentVariablesMap {

  private final Map<String, String> variables;

  private EnvironmentVariablesSystem(IdeContext context) {

    this(context, context.getSystem().getEnv());
  }

  private EnvironmentVariablesSystem(IdeContext context, Map<String, String> variables) {

    super(null, context);
    this.variables = variables;
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.SYSTEM;
  }

  @Override
  public Map<String, String> getVariables() {

    return this.variables;
  }

  @Override
  public String getFlat(String name) {

    for (VariableDefinition<?> variable : IdeVariables.VARIABLES) {
      if ((variable != IdeVariables.PATH) && (variable != IdeVariables.IDE_ROOT) && (variable != IdeVariables.HOME) && name.equals(variable.getName())) {
        return null;
      }
    }
    if (name.endsWith("_VERSION") || name.endsWith("_EDITION") || name.endsWith("_HOME")) {
      return null;
    }
    return super.getFlat(name);
  }

  /**
   * @param context the {@link IdeContext}.
   * @return the {@link EnvironmentVariablesSystem} instance.
   */
  static EnvironmentVariablesSystem of(IdeContext context) {

    return new EnvironmentVariablesSystem(context);
  }


  /**
   * @param context the {@link IdeContext}.
   * @param variables the mocked environment variables.
   * @return the {@link EnvironmentVariablesSystem} instance.
   */
  public static EnvironmentVariablesSystem of(IdeContext context, Map<String, String> variables) {

    return new EnvironmentVariablesSystem(context, variables);
  }
}
