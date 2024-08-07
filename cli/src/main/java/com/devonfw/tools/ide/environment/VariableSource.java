package com.devonfw.tools.ide.environment;

import java.nio.file.Path;

/**
 * {@link Record} for the {@link EnvironmentVariables#getSource() source} of {@link EnvironmentVariables} or {@link VariableLine}.
 *
 * @param type
 * @param properties
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
