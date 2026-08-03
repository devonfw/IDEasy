package com.devonfw.tools.ide.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Abstract base implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link List}.
 */
public abstract class AbstractVariableDefinitionList<E> extends AbstractVariableDefinition<List<E>> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractVariableDefinitionList.class);

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public AbstractVariableDefinitionList(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public AbstractVariableDefinitionList(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public AbstractVariableDefinitionList(String name, String legacyName,
      Function<IdeContext, List<E>> defaultValueFactory) {

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
  public AbstractVariableDefinitionList(String name, String legacyName,
      Function<IdeContext, List<E>> defaultValueFactory, boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Class<List<E>> getValueType() {

    return (Class) List.class;
  }

  @Override
  public List<E> fromString(String value, IdeContext context) {

    if (value.isEmpty()) {
      return Collections.emptyList();
    }
    return parseList(value, context);
  }

  protected List<E> parseList(String value, IdeContext context) {

    String[] items = value.split(",");
    if (items.length == 0) {
      return List.of();
    }
    List<E> list = new ArrayList<>(items.length);
    for (String item : items) {
      try {
        list.add(parseValue(item.trim(), context));
      } catch (Exception e) {
        LOG.warn("Invalid value '{}' for element of variable {}", item, getName(), e);
        return null;
      }

    }
    list = Collections.unmodifiableList(list);
    return list;
  }

  protected abstract E parseValue(String value, IdeContext context);

  @Override
  public String toString(List<E> value, IdeContext context) {

    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.size() * 5);
    for (E element : value) {
      if (!sb.isEmpty()) {
        sb.append(',');
      }
      sb.append(formatValue(element));
    }
    return sb.toString();
  }

  protected String formatValue(E value) {

    if (value == null) {
      return "";
    }
    return value.toString();
  }
}
