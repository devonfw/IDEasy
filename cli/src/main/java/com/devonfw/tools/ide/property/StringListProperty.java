package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link Property} with {@link #getValueType() value type} {@link String}.
 */
public class StringListProperty extends Property {

  private static final String[] NO_ARGS = new String[0];

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param multiValued
   */
  public StringListProperty(String name, boolean required, String alias, boolean multiValued) {

    this(name, required, alias, null, multiValued);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   * @param multiValued
   */
  public StringListProperty(String name, boolean required, String alias, Consumer<String> validator, boolean multiValued) {

    super(name, required, alias, validator, multiValued);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Class<String> getValueType() {

    if (isMultiValued()) {
      return (Class) List.class;
    } else {
      return String.class;
    }
  }

  @Override
  public void setValueAsString(String valueAsString, IdeContext context) {

    if (isMultiValued()) {
      Objects.requireNonNull(valueAsString);
      // pragmatic solution this implementation does not set the list value to the given string
      // instead it adds the given value to the list
      List<String> list = this.value;
      if (list == null) {
        this.value = new ArrayList<>();
      }
      list.add(valueAsString);
    } else {
      super.setValueAsString(valueAsString, context);
    }

  }

  @Override
  public List<String> parse(String valueAsString, IdeContext context) {

    if (isMultiValued()) {
      String[] items = valueAsString.split(" ");
      return Arrays.asList(items);
    } else {
      return null;
    }

  }

  /**
   * @return the {@link #getValue() value} as null-safe {@link String} array.
   */
  public String[] asArray() {

    List<String> list = getValue();
    //add some stuff
    if ((list == null) || list.isEmpty()) {
      return NO_ARGS;
    }
    return list.toArray(new String[0]);
  }

  public void setValue(List<String> value) {

    this.value = value;
  }

  @Override
  public List<String> getValue() {

    return this.value;
  }

  @Override
  protected boolean applyValue(String argValue, boolean lookahead, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    this.value.add(argValue);
    while (args.hasNext()) {
      CliArgument arg = args.next();
      this.value.add(arg.get());
    }
    args.next();
    return true;
  }

}
