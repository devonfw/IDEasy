package com.devonfw.tools.ide.environment;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * Implementation of {@link EnvironmentVariables}.
 */
abstract class EnvironmentVariablesMap extends AbstractEnvironmentVariables {

  private static final Logger LOG = LoggerFactory.getLogger(EnvironmentVariablesMap.class);

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
      LOG.trace("{}: Variable {} is undefined.", getSource(), name);
    } else {
      // the value is deliberately not logged since the variable may be a secret
      LOG.trace("{}: Variable {} is defined.", getSource(), name);
      WindowsPathSyntax pathSyntax = this.context.getPathSyntax();
      if (pathSyntax != null) {
        String normalized = pathSyntax.normalize(value);
        if (!value.equals(normalized)) {
          LOG.trace("Normalized value of variable {} using {}", name, pathSyntax);
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
