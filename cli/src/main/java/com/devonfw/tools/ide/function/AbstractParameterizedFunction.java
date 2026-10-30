package com.devonfw.tools.ide.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.validation.ValidationResult;
import com.devonfw.tools.ide.validation.ValidationState;

/**
 * Abstract base implementation of {@link ParameterizedFunction}.
 */
public abstract class AbstractParameterizedFunction implements ParameterizedFunction {

  private final List<Property<?>> propertiesList;

  private final List<Property<?>> properties;

  private Property<?> multiValued;

  protected AbstractParameterizedFunction() {
    super();
    this.propertiesList = new ArrayList<>();
    this.properties = Collections.unmodifiableList(this.propertiesList);
  }

  @Override
  public List<Property<?>> getProperties() {

    return this.properties;
  }

  /**
   * @param <P> type of the {@link Property}.
   * @param property the {@link Property} to register.
   * @return the given {@link Property}.
   */
  protected <P extends Property<?>> P add(P property) {

    if (this.multiValued != null) {
      throw new IllegalStateException("The multi-valued property " + this.multiValued + " can not be followed by " + property);
    }
    this.propertiesList.add(property);
    if (property.isOption()) {
      add(property.getName(), property, false);
      add(property.getAlias(), property, true);
    }
    if (property.isMultiValued()) {
      this.multiValued = property;
    }
    return property;
  }

  private void add(String name, Property<?> property, boolean alias) {

    if (alias && (name == null)) {
      return;
    }
    Objects.requireNonNull(name);
    assert (name.equals(name.trim()));
    if (name.isEmpty() && !alias) {
      return;
    }
    Property<?> duplicate = registerOption(name, property);
    if (duplicate != null) {
      throw new IllegalStateException("Duplicate name or alias " + name + " for " + property + " and " + duplicate);
    }
  }

  protected Property<?> registerOption(String name, Property<?> property) {
    return null;
  }

  /**
   * @return {@code true} if this {@link AbstractCommandlet} is the valid candidate to be {@link #run()}, {@code false} otherwise.
   * @see Property#validate()
   */
  @Override
  public ValidationResult validate() {
    ValidationState state = new ValidationState(null);
    // avoid validation exception if not a candidate to be run.
    for (Property<?> property : this.propertiesList) {
      state.add(property.validate());
    }
    return state;
  }

  /**
   * Clear the set values on all properties of the {@link AbstractCommandlet#propertiesList}
   */
  @Override
  public void reset() {

    for (Property<?> property : this.propertiesList) {
      property.clearValue();
    }
  }

}
