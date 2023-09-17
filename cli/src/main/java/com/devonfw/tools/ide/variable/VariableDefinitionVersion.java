package com.devonfw.tools.ide.variable;

import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type}
 * {@link VersionIdentifier}.
 */
public class VariableDefinitionVersion extends AbstractVariableDefinition<VersionIdentifier> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionVersion(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionVersion(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionVersion(String name, String legacyName,
      Function<IdeContext, VersionIdentifier> defaultValueFactory) {

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
  public VariableDefinitionVersion(String name, String legacyName,
      Function<IdeContext, VersionIdentifier> defaultValueFactory, boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  @Override
  public Class<VersionIdentifier> getValueType() {

    return VersionIdentifier.class;
  }

  @Override
  public VersionIdentifier fromString(String value, IdeContext context) {

    return VersionIdentifier.of(value);
  }
}
