package com.devonfw.tools.ide.process;

import java.nio.file.Path;
import java.util.Map;

import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * Implementation of {@link EnvironmentContext}. This class collects {@link EnvironmentVariables} and sets them in the {@link EnvironmentContext}, if not
 * already existent.
 */
public class EnvironmentVariableCollectorContext implements EnvironmentContext {

  private final Map<String, VariableLine> environmentVariables;

  private final VariableSource variableSource;

  private final WindowsPathSyntax pathSyntax;

  /**
   * The constructor.
   *
   * @param environmentVariables A Map of {@link EnvironmentVariables} that will be added to the {@link EnvironmentContext}.
   * @param variableSource Describes {@link VariableSource} of the {@link EnvironmentVariables}.
   * @param pathSyntax The {@link WindowsPathSyntax} determines the format of a path for the output.
   */
  public EnvironmentVariableCollectorContext(Map<String, VariableLine> environmentVariables, VariableSource variableSource, WindowsPathSyntax pathSyntax) {
    super();
    this.environmentVariables = environmentVariables;
    this.variableSource = variableSource;
    this.pathSyntax = pathSyntax;
  }

  @Override
  public EnvironmentContext withEnvVar(String key, String value) {
    if (this.pathSyntax != null) {
      value = pathSyntax.normalize(value);
    }
    if (!this.environmentVariables.containsKey(key)) {
      environmentVariables.put(key, VariableLine.of(true, key, value, variableSource));
    }
    return this;
  }

  @Override
  public EnvironmentContext withPathEntry(Path path) {
    return this;
  }

}
