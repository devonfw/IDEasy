package com.devonfw.tools.ide.variable;

import java.util.Locale;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link Boolean}.
 */
public class VariableDefinitionBoolean extends AbstractVariableDefinition<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(VariableDefinitionBoolean.class);

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionBoolean(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionBoolean(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionBoolean(String name, String legacyName, Function<IdeContext, Boolean> defaultValueFactory) {

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
  public VariableDefinitionBoolean(String name, String legacyName, Function<IdeContext, Boolean> defaultValueFactory, boolean forceDefaultValue) {

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
  public VariableDefinitionBoolean(String name, String legacyName, Function<IdeContext, Boolean> defaultValueFactory, boolean forceDefaultValue,
      boolean export) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue, export);
  }

  @Override
  public Class<Boolean> getValueType() {

    return Boolean.class;
  }

  @Override
  public Boolean fromString(String value, IdeContext context) {

    String lower = value.trim().toLowerCase(Locale.ROOT);
    return switch (lower) {
      case "true", "yes" -> Boolean.TRUE;
      case "false", "no" -> Boolean.FALSE;
      default -> {
        LOG.warn("Variable {} has invalid boolean value {} - using false as fallback", getName(), value);
        yield Boolean.FALSE;
      }
    };
  }

}
