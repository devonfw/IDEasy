package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

/**
 * {@link Record} for the {@link EnvironmentVariables#getSource() source} of {@link EnvironmentVariables} or {@link VariableLine}.
 *
 * @param type the {@link EnvironmentVariablesType}.
 * @param properties the optional {@link Path} to the properties file in case the {@link EnvironmentVariables#getSource() source} is from a properties
 *     file.
 */
public record VariableSource(EnvironmentVariablesType type, Path properties) {

  @Override
  public String toString() {
    if (this.properties != null) {
      return this.type + "@" + this.properties;
    }
    return this.type.toString();
  }

}
