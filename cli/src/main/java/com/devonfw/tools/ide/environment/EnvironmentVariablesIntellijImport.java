package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

/**
 * Custom environment variables class used for automatic project import in intellij
 */
public class EnvironmentVariablesIntellijImport extends AbstractEnvironmentVariables {

  Path projectPath;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param repositoryPath path to repository in current workspace
   */
  public EnvironmentVariablesIntellijImport(AbstractEnvironmentVariables parent, Path repositoryPath) {
    super(parent, parent.context);
    this.projectPath = repositoryPath;
    System.out.println("Creating a new instance of EnvironmentVariablesIntellijImport!");
  }

  @Override
  public String getFlat(String name) {

    return null;
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.RESOLVED;
  }

  @Override
  protected void collectVariables(Map<String, VariableLine> variables, boolean onlyExported, AbstractEnvironmentVariables resolver) {
    System.out.println("EnvironmentVariablesIntelljImport.collectVariables called!");
    for (VariableDefinition<?> var : IdeVariables.VARIABLES) {
      System.out.println(var);
    }
    super.collectVariables(variables, onlyExported, resolver);
  }

  @Override
  protected String getValue(String name, boolean ignoreDefaultValue) {
    System.out.println("getting the value of " + name);
    if (Objects.equals(name, "PROJECT_PATH")) {
      return this.projectPath.toString();
    }
    return super.getValue(name, ignoreDefaultValue);
  }
}
