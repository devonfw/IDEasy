package com.devonfw.tools.ide.property;

import java.util.Locale;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Property} with {@link #getValueType() value type} {@link Boolean}.
 *
 * @param <V> type of the {@link Enum} {@link #getValue() value}.
 */
public class EnumProperty<V extends Enum<V>> extends Property<V> {

  private final Class<V> valueType;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param valueType the {@link #getValueType() value type}.
   */
  public EnumProperty(String name, boolean required, String alias, Class<V> valueType) {

    super(name, required, alias);
    this.valueType = valueType;
  }

  @Override
  public Class<V> getValueType() {

    return this.valueType;
  }

  @Override
  public V parse(String valueAsString, IdeContext context) {

    for (V enumConstant : this.valueType.getEnumConstants()) {
      String name = enumConstant.name().toLowerCase(Locale.ROOT);
      if (name.equals(valueAsString)) {
        return enumConstant;
      }
    }

    throw new IllegalArgumentException(String.format("Invalid Enum option: %s", valueAsString));
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    for (V enumConstant : this.valueType.getEnumConstants()) {
      String name = enumConstant.name().toLowerCase(Locale.ROOT);
      if (name.startsWith(arg)) {
        collector.add(name, null, this, commandlet);
      }
    }
  }
}
