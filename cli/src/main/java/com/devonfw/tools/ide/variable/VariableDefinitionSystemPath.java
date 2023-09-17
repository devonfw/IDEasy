package com.devonfw.tools.ide.variable;

import java.nio.file.Path;
import java.util.function.Function;

import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link Path}.
 */
public class VariableDefinitionSystemPath extends AbstractVariableDefinition<SystemPath> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionSystemPath(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionSystemPath(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionSystemPath(String name, String legacyName,
      Function<IdeContext, SystemPath> defaultValueFactory) {

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
  public VariableDefinitionSystemPath(String name, String legacyName,
      Function<IdeContext, SystemPath> defaultValueFactory, boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  @Override
  public Class<SystemPath> getValueType() {

    return SystemPath.class;
  }

  @Override
  public SystemPath fromString(String value, IdeContext context) {

    return new SystemPath(value, context.getSoftwarePath());
  }
}
