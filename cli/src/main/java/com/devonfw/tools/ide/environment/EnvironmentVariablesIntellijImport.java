package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.Objects;

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
  protected String getValue(String name, boolean ignoreDefaultValue) {
    System.out.println("getting the value of " + name);
    if (Objects.equals(name, "PROJECT_PATH")) {
      return this.projectPath.toString();
    }
    return super.getValue(name, ignoreDefaultValue);
  }
}
