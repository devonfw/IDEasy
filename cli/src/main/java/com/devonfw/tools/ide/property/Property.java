package com.devonfw.tools.ide.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorAdapter;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.validation.PropertyValidator;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationState;

/**
 * A {@link Property} is a simple container for a {@link #getValue() value} with a fixed {@link #getName() name} and {@link #getValueType() type}. Further we
 * use a {@link Property} as {@link CliArgument CLI argument} so it is either an {@link #isOption() option} or a {@link #isValue() value}.<br> In classic Java
 * Beans a property only exists implicit as a combination of a getter and a setter. This class makes it an explicit construct that allows to
 * {@link #getValue() get} and {@link #setValue(Object) set} the value of a property easily in a generic way including to retrieve its {@link #getName() name}
 * and {@link #getValueType() type}. Besides simplification this also prevents the use of reflection for generic CLI parsing with assigning and validating
 * arguments what is beneficial for compiling the Java code to a native image using GraalVM.
 *
 * @param <V> the {@link #getValueType() value type}.
 */
public abstract class Property<V> {

  private static final Logger LOG = LoggerFactory.getLogger(Property.class);

  private static final String INVALID_ARGUMENT = "Invalid CLI argument '{}' for property '{}' of commandlet '{}'";

  private static final String INVALID_ARGUMENT_WITH_EXCEPTION_MESSAGE = INVALID_ARGUMENT + ": {}";

  /** @see #getName() */
  protected final String name;

  /** @see #getAlias() */
  protected final String alias;

  /** @see #isRequired() */
  protected final boolean required;

  private final PropertyValidator<V> validator;

  /** @see #isMultiValued() */
  private final boolean multivalued;

  /** @see #getValue() */
  protected final List<V> value = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public Property(String name, boolean required, String alias) {

    super();
    this.name = name;
    this.required = required;
    this.alias = alias;
    this.multivalued = false;
    this.validator = null;
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param multivalued the boolean flag about multiple arguments
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public Property(String name, boolean required, String alias, boolean multivalued, PropertyValidator<V> validator) {

    super();
    this.name = name;
    this.required = required;
    this.alias = alias;
    this.validator = validator;
    this.multivalued = multivalued;
  }

  /**
   * @return the name of this property. Will be the empty {@link String} for a {@link #isValue() value} property that is not a keyword.
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
   * @return the {@link #getName() name} or the {@link #getAlias() alias} if {@link #getName() name} is {@link String#isEmpty() empty}.
   */
  public String getNameOrAlias() {

    if (this.name.isEmpty()) {
      return this.alias;
    }
    return this.name;
  }

  /**
   * @return {@code true} if this property is required (if argument is not present the {@link Commandlet} cannot be invoked), {@code false} otherwise (if
   *     optional).
   */
  public boolean isRequired() {

    return this.required;
  }

  /**
   * @return {@code true} if a value is expected as additional CLI argument.
   */
  public boolean isExpectValue() {

    return "".equals(this.name);
  }

  /**
   * Determines if this {@link Property} is an option. Canonical options have a long-option {@link #getName() name} (e.g. "--force") and a short-option
   * {@link #getAlias() alias} (e.g. "-f").
   *
   * @return {@code true} if this {@link Property} is an option, {@code false} otherwise (if a positional argument).
   */
  public boolean isOption() {

    return this.name.startsWith("-");
  }

  /**
   * @return {@code true} if this {@link Property} forces an implicit {@link CliArgument#isEndOptions() end-options} as if "--" was provided before its first
   *     {@link CliArgument argument}.
   */
  public boolean isEndOptions() {

    return isMultiValued();
  }

  /**
   * Determines if this {@link Property} is multi-valued and accepts any number of values. A multi-valued {@link Property} needs to be the last {@link Property}
   * of a {@link Commandlet}.
   *
   * @return {@code true} if multi-valued, {@code false} otherwise.
   */
  public boolean isMultiValued() {

    return this.multivalued;
  }

  /**
   * Determines if this a value {@link Property}. Such value is either a {@link KeywordProperty} with the keyword as {@link #getName() name} or a raw indexed
   * value argument. In the latter case the command-line argument at this index will be the immediate value of the {@link Property}, the {@link #getName() name}
   * is {@link String#isEmpty() empty} and the {@link #getAlias() alias} is a logical name of the value to display to users.
   *
   * @return {@code true} if value, {@code false} otherwise.
   */
  public boolean isValue() {

    return !isOption();
  }

  /**
   * @return the {@link Class} reflecting the type of the {@link #getValue() value}.
   */
  public abstract Class<V> getValueType();

  /**
   * @return the value of this property.
   * @see #setValue(Object)
   */
  public V getValue() {

    if (this.value.isEmpty()) {
      return null;
    } else {
      return this.value.getFirst();
    }
  }

  /**
   * @param i the position to get.
   * @return the value of this property.
   */
  public V getValue(int i) {

    return this.value.get(i);
  }

  /**
   * @return amount of values.
   */
  public int getValueCount() {

    return this.value.size();
  }

  /**
   * @return the {@link #getValue() value} as {@link String}.
   * @see #setValueAsString(String, IdeContext)
   */
  public String getValueAsString() {

    if (getValue() == null) {
      return null;
    }
    return format(getValue());
  }

  /**
   * @return a {@link List} containing all {@link #getValue(int) values}. This method only makes sense for {@link #isMultiValued() multi valued} properties.
   */
  public List<V> asList() {

    return new ArrayList<>(this.value);
  }

  /**
   * @param valueToFormat the value to format.
   * @return the given {@code value} formatted as {@link String}.
   */
  protected String format(V valueToFormat) {

    return valueToFormat.toString();
  }

  /**
   * @param value the new {@link #getValue() value} to set.
   * @see #getValue()
   */
  public void setValue(V value) {

    if (!this.multivalued) {
      this.value.clear();
    }
    this.value.add(value);
  }

  /**
   * Clears the {@link #value value} list.
   */
  public void clearValue() {

    this.value.clear();
  }

  /**
   * @param value the value to add to the {@link List} of values.
   * @see #isMultiValued()
   */
  public void addValue(V value) {

    if (!this.multivalued) {
      throw new IllegalStateException("not multivalued");
    }
    this.value.add(value);
  }

  /**
   * @param value the new {@link #getValue() value} to set.
   * @param i the position to set.
   */
  public void setValue(V value, int i) {

    this.value.set(i, value);
  }

  /**
   * @param valueAsString the new {@link #getValue() value} as {@link String}.
   * @param context the {@link IdeContext}
   * @see #getValueAsString()
   */
  public void setValueAsString(String valueAsString, IdeContext context) {

    if (valueAsString == null) {
      setValue(getNullValue());
    } else {
      setValue(parse(valueAsString, context));
    }
  }

  /**
   * Like {@link #setValueAsString(String, IdeContext)} but with exception handling.
   *
   * @param valueAsString the new {@link #getValue() value} as {@link String}.
   * @param context the {@link IdeContext}
   * @param commandlet the {@link Commandlet} owning this property.
   * @return {@code true} if the value has been assigned successfully, {@code false} otherwise (an error occurred).
   */
  public final boolean assignValueAsString(String valueAsString, IdeContext context, Commandlet commandlet) {

    try {
      setValueAsString(valueAsString, context);
      return true;
    } catch (Exception e) {
      if (e instanceof IllegalArgumentException) {
        LOG.warn(INVALID_ARGUMENT, valueAsString, getNameOrAlias(), commandlet.getName());
      } else {
        LOG.warn(INVALID_ARGUMENT_WITH_EXCEPTION_MESSAGE, valueAsString, getNameOrAlias(), commandlet.getName(), e.getMessage());
      }
      return false;
    }
  }

  /**
   * @return the {@code null} value.
   */
  protected V getNullValue() {

    return null;
  }

  /**
   * @param valueAsString the value to parse given as {@link String}.
   * @param context the {@link IdeContext}.
   * @return the parsed value.
   */
  public abstract V parse(String valueAsString, IdeContext context);

  /**
   * @param args the {@link CliArguments} already {@link CliArguments#current() pointing} the {@link CliArgument} to apply.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link Commandlet} owning this property.
   * @param collector the {@link CompletionCandidateCollector}.
   * @return {@code true} if it matches, {@code false} otherwise.
   */
  public boolean apply(CliArguments args, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    return apply(this.name, args, context, commandlet, collector);
  }

  /**
   * @param normalizedName the {@link #getName() name} or potentially a normalized form of it (see {@link KeywordProperty}).
   * @param args the {@link CliArguments} already {@link CliArguments#current() pointing} the {@link CliArgument} to apply.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link Commandlet} owning this property.
   * @param collector the {@link CompletionCandidateCollector}.
   * @return {@code true} if it matches, {@code false} otherwise.
   */
  protected boolean apply(String normalizedName, CliArguments args, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    CliArgument argument = args.current();
    if (argument.isCompletion()) {
      int size = collector.getCandidates().size();
      complete(normalizedName, argument, args, context, commandlet, collector);
      return (collector.getCandidates().size() > size);
    }
    boolean option = normalizedName.startsWith("-");
    if (option && !argument.isOption()) {
      return false;
    }
    if (!option && argument.isOption() && (argument.get().length() > 1) && args.isSplitShortOpts()) {
      return false;
    }
    String argValue = null;
    boolean lookahead = false;
    if (normalizedName.isEmpty()) {
      argValue = argument.get();
    } else {
      if (!matches(argument.getKey())) {
        return false;
      }
      argValue = argument.getValue();
      if (argValue == null) {
        argument = args.next();
        if (argument.isCompletion()) {
          completeValue(argument.get(), context, commandlet, collector);
          return true;
        } else {
          if (!argument.isEnd()) {
            argValue = argument.get();
          }
          lookahead = true;
        }
      }
    }
    return applyValue(argValue, lookahead, args, context, commandlet, collector);
  }

  /**
   * @param argValue the value to set as {@link String}.
   * @param lookahead - {@code true} if the given {@code argValue} is taken as lookahead from the next value, {@code false} otherwise.
   * @param args the {@link CliArguments}.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link Commandlet} owning this {@link Property}.
   * @param collector the {@link CompletionCandidateCollector}.
   * @return {@code true} if it matches, {@code false} otherwise.
   */
  protected boolean applyValue(String argValue, boolean lookahead, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    boolean success = assignValueAsString(argValue, context, commandlet);

    if (success) {
      if (this.multivalued) {
        while (success && args.hasNext()) {
          CliArgument arg = args.next();
          success = assignValueAsString(arg.get(), context, commandlet);
        }
      }
    }
    args.next();
    return success;
  }

  /**
   * Performs auto-completion for the {@code arg}.
   *
   * @param normalizedName the {@link #getName() name} or potentially a normalized form of it (see {@link KeywordProperty}).
   * @param argument the {@link CliArgument CLI argument}.
   * @param args the {@link CliArguments}.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link Commandlet} owning this {@link Property}.
   * @param collector the {@link CompletionCandidateCollector}.
   */
  protected void complete(String normalizedName, CliArgument argument, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    String arg = argument.get();
    if (normalizedName.isEmpty()) {
      int count = collector.getCandidates().size();
      completeValue(arg, context, commandlet, collector);
      if (collector.getCandidates().size() > count) {
        args.next();
      }
      return;
    }
    if (normalizedName.startsWith(arg)) {
      collector.add(normalizedName, null, this, commandlet);
    }
    if (this.alias != null) {
      if (this.alias.startsWith(arg)) {
        collector.add(this.alias, null, this, commandlet);
      } else if ((this.alias.length() == 2) && (this.alias.charAt(0) == '-') && argument.isShortOption()) {
        char opt = this.alias.charAt(1); // e.g. arg="-do" and alias="-f" -complete-> "-dof"
        if (arg.indexOf(opt) < 0) {
          collector.add(arg + opt, null, this, commandlet);
        }
      }
    }
    String value = argument.getValue();
    if (value != null) {
      String key = argument.getKey();
      if (normalizedName.equals(key) || Objects.equals(this.alias, key)) {
        completeValue(value, context, commandlet, new CompletionCandidateCollectorAdapter(key + "=", collector));
      }
    }
  }

  /**
   * Performs auto-completion for the {@code arg} as {@link #getValue() property value}.
   *
   * @param arg the {@link CliArgument#get() CLI argument}.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link Commandlet} owning this {@link Property}.
   * @param collector the {@link CompletionCandidateCollector}.
   */
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

  }

  /**
   * @param nameOrAlias the potential {@link #getName() name} or {@link #getAlias() alias} to match.
   * @return {@code true} if the given {@code nameOrAlias} is equal to {@link #getName() name} or {@link #getAlias() alias}, {@code false} otherwise.
   */
  public boolean matches(String nameOrAlias) {

    return this.name.equals(nameOrAlias) || Objects.equals(this.alias, nameOrAlias);
  }

  /**
   * @return {@code true} if this {@link Property} is valid, {@code false} if it is {@link #isRequired() required} but no {@link #getValue() value} has been
   *     set.
   * @throws RuntimeException if the {@link #getValue() value} is violating given constraints. This is checked by the optional {@link Consumer} function
   *     given at construction time.
   */
  public ValidationResult validate() {

    ValidationState state = new ValidationState(this.getNameOrAlias());

    if (this.required && (getValue() == null)) {
      state.addErrorMessage("Value is required and cannot be empty.");
      return state;
    }
    if (this.validator != null) {
      for (V value : this.value) {
        validator.validate(value, state);
      }
    }
    return state;
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.name, this.value);
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
