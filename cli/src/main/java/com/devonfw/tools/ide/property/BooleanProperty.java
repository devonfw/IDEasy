package com.devonfw.tools.ide.property;

import java.util.Objects;

/**
 * {@link Property} with {@link #getValueType() value type} {@link Boolean}.
 */
public class BooleanProperty extends Property<Boolean> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public BooleanProperty(String name, boolean required, String alias) {

    super(name, required, alias, null);
  }

  @Override
  public Class<Boolean> getValueType() {

    return Boolean.class;
  }

  /**
   * @return the value as primitive boolean.
   */
  public boolean isTrue() {

    return Boolean.TRUE.equals(getValue());
  }

  @Override
  public Boolean parse(String valueAsString) {

    valueAsString = valueAsString.toLowerCase();
    if ("true".equals(valueAsString) || "yes".equals(valueAsString)) {
      return Boolean.TRUE;
    } else if ("false".equals(valueAsString) || "no".equals(valueAsString)) {
      return Boolean.FALSE;
    }
    throw new IllegalArgumentException("Illegal boolean value '" + valueAsString + "' for property " + getName());
  }

  @Override
  public void setValueAsString(String valueAsString) {

    Boolean value;
    if (matches(valueAsString)) {
      // allow "--force" to enable "--force" option
      value = Boolean.TRUE;
    } else {
      value = parse(valueAsString);
    }
    setValue(value);
  }

  /**
   * @param nameOrAlias the potential {@link #getName() name} or {@link #getAlias() alias}.
   * @return {@code true} if it matches, {@code false} otherwise.
   */
  public boolean matches(String nameOrAlias) {

    return (this.name.equals(nameOrAlias) || Objects.equals(this.alias, nameOrAlias));
  }

}
