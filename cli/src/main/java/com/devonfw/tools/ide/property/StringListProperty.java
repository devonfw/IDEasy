package com.devonfw.tools.ide.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link Property} with {@link #getValueType() value type} {@link String}.
 */
public class StringListProperty extends Property<List<String>> {

  private static final String[] NO_ARGS = new String[0];

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public StringListProperty(String name, boolean required, String alias) {

    this(name, required, alias, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public StringListProperty(String name, boolean required, String alias, Consumer<List<String>> validator) {

    super(name, required, alias, validator);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Class<List<String>> getValueType() {

    return (Class) List.class;
  }

  @Override
  public boolean isMultiValued() {

    return true;
  }

  @Override
  public void setValueAsString(String valueAsString) {

    Objects.requireNonNull(valueAsString);
    // pragmatic solution this implementation does not set the list value to the given string
    // instead it adds the given value to the list
    List<String> list = getValue();
    if (list == null) {
      list = new ArrayList<>();
      setValue(list);
    }
    list.add(valueAsString);
  }

  @Override
  public List<String> parse(String valueAsString) {

    String[] items = valueAsString.split(" ");
    return Arrays.asList(items);
  }

  /**
   * @return the {@link #getValue() value} as null-safe {@link String} array.
   */
  public String[] asArray() {

    List<String> list = getValue();
    if ((list == null) || list.isEmpty()) {
      return NO_ARGS;
    }
    return list.toArray(new String[list.size()]);
  }

}
