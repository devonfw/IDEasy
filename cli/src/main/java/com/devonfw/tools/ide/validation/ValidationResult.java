package com.devonfw.tools.ide.validation;

/**
 * Interface for the result of a validation.
 */
public interface ValidationResult {

  /**
   * @return {@code true} if the validation was successful, {@code false} otherwise (validation constraint(s) violated).
   */
  boolean isValid();

  /**
   * @return the error messsage(s) of the validation or {@code null} if {@link #isValid() valid}.
   */
  String getErrorMessage();

}
