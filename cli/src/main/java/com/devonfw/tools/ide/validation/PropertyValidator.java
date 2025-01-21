package com.devonfw.tools.ide.validation;

/**
 * {@link FunctionalInterface} for validator of a {@link com.devonfw.tools.ide.property.Property}.
 *
 * @param <V> type of the {@link com.devonfw.tools.ide.property.Property#getValue() property value}.
 */
@FunctionalInterface
public interface PropertyValidator<V> {

  /**
   * @param value the value to validate.
   * @param validationState the {@link ValidationState} where error messages can be added.
   */
  void validate(V value, ValidationState validationState);

}
