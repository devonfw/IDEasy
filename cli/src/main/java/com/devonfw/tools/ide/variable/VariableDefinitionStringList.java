package com.devonfw.tools.ide.variable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link String}.
 */
public class VariableDefinitionStringList extends AbstractVariableDefinition<List<String>> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionStringList(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionStringList(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionStringList(String name, String legacyName,
      Function<IdeContext, List<String>> defaultValueFactory) {

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
  public VariableDefinitionStringList(String name, String legacyName,
      Function<IdeContext, List<String>> defaultValueFactory, boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Class<List<String>> getValueType() {

    return (Class) List.class;
  }

  @Override
  public List<String> fromString(String value, IdeContext context) {

    if (value.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> list = VariableLine.parseArray(value);
    list = Collections.unmodifiableList(list);
    return list;
  }

  private boolean isBashArray(String value) {

    return value.startsWith("(") && value.endsWith(")");
  }

  @Override
  public VariableLine migrateLine(VariableLine line) {

    line = super.migrateLine(line);
    String value = line.getValue();
    if ((value != null) && isBashArray(value)) {
      List<String> list = fromString(value, null);
      line = line.withValue(String.join(", ", list));
    }
    return line;
  }

  @Override
  public String toString(List<String> value, IdeContext context) {

    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.size() * 5);
    for (Object element : value) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(element);
    }
    return sb.toString();
  }
}
