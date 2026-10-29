package com.devonfw.tools.ide.function;

import java.util.List;

import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.validation.ValidationResult;

/**
 * Abstract interface for an executable function that has parameters. It is either a {@link com.devonfw.tools.ide.commandlet.Commandlet} or an
 * {@link com.devonfw.tools.ide.expression.ExpressionFunction}.
 */
public abstract interface ParameterizedFunction {

  /**
   * @return the name of this function.
   */
  String getName();

  /**
   * @return the {@link List} with all {@link Property properties} for the parameters of this function.
   */
  List<Property<?>> getProperties();

  /**
   * @return the {@link ValidationResult} of the validation of this function. Will determine if this function with its {@link #getProperties() parameters} is
   *     valid for execution.
   */
  ValidationResult validate();

  /**
   * Clear the set values on all {@link #getProperties() properties} of this function.
   */
  void reset();

}
