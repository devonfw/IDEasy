package com.devonfw.tools.ide.environment;

import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * Implementation of {@link EnvironmentVariables}.
 */
abstract class EnvironmentVariablesMap extends AbstractEnvironmentVariables {

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param context the {@link IdeContext}.
   */
  EnvironmentVariablesMap(AbstractEnvironmentVariables parent, IdeContext context) {

    super(parent, context);
  }

  /**
   * @return the {@link Map} with the underlying variables. Internal method do not call from outside and never manipulate this {@link Map} externally.
   */
  protected abstract Map<String, String> getVariables();

  @Override
  public String getFlat(String name) {

    String value = getVariables().get(name);
    if (value == null) {
      this.context.trace("{}: Variable {} is undefined.", getSource(), name);
    } else {
      this.context.trace("{}: Variable {}={}", getSource(), name, value);
      WindowsPathSyntax pathSyntax = this.context.getPathSyntax();
      if (pathSyntax != null) {
        String normalized = pathSyntax.normalize(value);
        if (!value.equals(normalized)) {
          this.context.trace("Normalized {} using {} to {}", value, pathSyntax, normalized);
          value = normalized;
        }
      }
    }
    return value;
  }

  @Override
  public String toString() {

    return getSource() + ":\n" + getVariables();
  }

}
