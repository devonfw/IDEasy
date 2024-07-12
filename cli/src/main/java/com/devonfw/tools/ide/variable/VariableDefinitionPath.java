package com.devonfw.tools.ide.variable;

import java.nio.file.Path;
import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link Path}.
 */
public class VariableDefinitionPath extends AbstractVariableDefinition<Path> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionPath(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionPath(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionPath(String name, String legacyName, Function<IdeContext, Path> defaultValueFactory) {

    super(name, legacyName, defaultValueFactory);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   * @param forceDefaultValue the {@link #isForceDefaultValue() forceDefaultValue} flag.
   */
  public VariableDefinitionPath(String name, String legacyName, Function<IdeContext, Path> defaultValueFactory, boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   * @param forceDefaultValue the {@link #isForceDefaultValue() forceDefaultValue} flag.
   * @param export the {@link #isExport() export} flag.
   */
  public VariableDefinitionPath(String name, String legacyName, Function<IdeContext, Path> defaultValueFactory, boolean forceDefaultValue, boolean export) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue, export);
  }

  @Override
  public Class<Path> getValueType() {

    return Path.class;
  }

  @Override
  public Path fromString(String value, IdeContext context) {

    return Path.of(value);
  }

  @Override
  public String toString(Path value, IdeContext context) {
    WindowsPathSyntax pathSyntax = context.getPathSyntax();
    if (pathSyntax != null) {
      return pathSyntax.format(value);
    } else {
      // avoid backslashes since they can cause trouble in files like *.properties since they are treated as escape char
      return value.toString().replace('\\', '/');
    }
  }
}
