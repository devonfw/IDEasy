package com.devonfw.tools.ide.property;

import java.util.Objects;
import java.util.function.Consumer;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.commandlet.Commandlet;

/**
 * A {@link Property} is a simple container for a {@link #getValue() value} with a fixed {@link #getName() name} and
 * {@link #getValueType() type}. Further we use a {@link Property} as {@link CliArgument CLI argument} so it is either
 * an {@link #isOption() option} or a {@link #isValue() value}.<br>
 * In classic Java Beans a property only exists implicit as a combination of a getter and a setter. This class makes it
 * an explicit construct that allows to {@link #getValue() get} and {@link #setValue(Object) set} the value of a
 * property easily in a generic way including to retrieve its {@link #getName() name} and {@link #getValueType() type}.
 * Besides simplification this also prevents the use of reflection for generic CLI parsing with assigning and validating
 * arguments what is beneficial for compiling the Java code to a native image using GraalVM.
 *
 * @param <T> the {@link #getValueType() value type}.
 */
public abstract class Property<T> {

  /** @see #getName() */
  protected final String name;

  /** @see #getAlias() */
  protected final String alias;

  /** @see #isRequired() */
  protected final boolean required;

  private final Consumer<T> validator;

  private T value;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public Property(String name, boolean required, String alias, Consumer<T> validator) {

    super();
    this.name = name;
    this.required = required;
    this.alias = alias;
    this.validator = validator;
  }

  /**
   * @return the name of this property.
   */
  public String getName() {

    return this.name;
  }

  /**
   * @return the alias of this property or {@code null} for none.
   * @see #isOption()
   */
  public String getAlias() {

    return this.alias;
  }

  /**
   * @return the {@link #getName() name} or the {@link #getAlias() alias} if {@link #getName() name} is
   *         {@link String#isEmpty() empty}.
   */
  public String getNameOrAlias() {

    if (this.name.isEmpty()) {
      return this.alias;
    }
    return this.name;
  }

  /**
   * @return {@code true} if this property is required (if argument is not present the {@link Commandlet} cannot be
   *         invoked), {@code false} otherwise (if optional).
   */
  public boolean isRequired() {

    return this.required;
  }

  /**
   * @return {@code true} if a value is expected as additional CLI argument.
   */
  public boolean isExpectValue() {

    return !"".equals(this.name);
  }

  /**
   * Determines if this {@link Property} is an option. Canonical options have a long-option {@link #getName() name}
   * (e.g. "--force") and a short-option {@link #getAlias() alias} (e.g. "-f").
   *
   * @return {@code true} if this {@link Property} is an option, {@code false} otherwise (if a positional argument).
   */
  public boolean isOption() {

    return this.name.startsWith("-");
  }

  /**
   * @return {@code true} if this {@link Property} forces an implicit {@link CliArgument#isEndOptions() end-options} as
   *         if "--" was provided before its first {@link CliArgument argument}.
   */
  public boolean isEndOptions() {

    return isMultiValued();
  }

  /**
   * Determines if this {@link Property} is multi-valued and accepts any number of values. A multi-valued
   * {@link Property} needs to be the last {@link Property} of a {@link Commandlet}.
   *
   * @return {@code true} if multi-valued, {@code false} otherwise.
   */
  public boolean isMultiValued() {

    return false;
  }

  /**
   * Determines if this a value {@link Property}. Such value is either a {@link KeywordProperty} with the keyword as
   * {@link #getName() name} or a raw indexed value argument. In the latter case the command-line argument at this index
   * will be the immediate value of the {@link Property}, the {@link #getName() name} is {@link String#isEmpty() empty}
   * and the {@link #getAlias() alias} is a logical name of the value to display to users.
   *
   * @return {@code true} if value, {@code false} otherwise.
   */
  public boolean isValue() {

    return !isOption();
  }

  /**
   * @return the {@link Class} reflecting the type of the {@link #getValue() value}.
   */
  public abstract Class<T> getValueType();

  /**
   * @return the value of this property.
   * @see #setValue(Object)
   */
  public T getValue() {

    return this.value;
  }

  /**
   * @return the {@link #getValue() value} as {@link String}.
   * @see #setValueAsString(String)
   */
  public String getValueAsString() {

    if (this.value == null) {
      return null;
    }
    return format(this.value);
  }

  /**
   * @param valueToFormat the value to format.
   * @return the given {@code value} formatted as {@link String}.
   */
  protected String format(T valueToFormat) {

    return this.value.toString();
  }

  /**
   * @param value the new {@link #getValue() value} to set.
   * @see #getValue()
   */
  public void setValue(T value) {

    this.value = value;
  }

  /**
   * @param valueAsString the new {@link #getValue() value} as {@link String}.
   * @see #getValueAsString()
   */
  public void setValueAsString(String valueAsString) {

    if (valueAsString == null) {
      this.value = getNullValue();
    } else {
      this.value = parse(valueAsString);
    }
  }

  /**
   * @return the {@code null} value.
   */
  protected T getNullValue() {

    return null;
  }

  /**
   * @param valueAsString the value to parse given as {@link String}.
   * @return the parsed value.
   */
  public abstract T parse(String valueAsString);

  @Override
  public int hashCode() {

    return Objects.hash(this.name, this.value);
  }

  /**
   * @return {@code true} if this {@link Property} is valid, {@code false} if it is {@link #isRequired() required} but
   *         no {@link #getValue() value} has been set.
   * @throws RuntimeException if the {@link #getValue() value} is violating given constraints. This is checked by the
   *         optional {@link Consumer} function given at construction time.
   */
  public boolean validate() {

    if (this.required && (this.value == null)) {
      return false;
    }
    if (this.validator != null) {
      this.validator.accept(this.value);
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    } else if ((obj == null) || (obj.getClass() != getClass())) {
      return false;
    }
    Property<?> other = (Property<?>) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    } else if (!Objects.equals(this.value, other.value)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");
    if (this.name.isEmpty()) {
      sb.append(this.alias);
    } else {
      sb.append(this.name);
      if (this.alias != null) {
        sb.append(" | ");
        sb.append(this.alias);
      }
    }
    sb.append(":");
    sb.append(getValueAsString());
    sb.append("]");
    return sb.toString();
  }

}
