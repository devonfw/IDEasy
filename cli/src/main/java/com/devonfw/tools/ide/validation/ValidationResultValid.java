package com.devonfw.tools.ide.validation;

/**
 * Implementation of {@link ValidationResult} that is always {@link #isValid() valid} and has no {@link #getErrorMessage() error message}.
 */
public class ValidationResultValid implements ValidationResult {

  private static final ValidationResultValid INSTANCE = new ValidationResultValid();

  @Override
  public boolean isValid() {

    return true;
  }

  @Override
  public String getErrorMessage() {

    return null;
  }

  /**
   * @return the singleton instance of {@link ValidationResultValid}.
   */
  public static ValidationResultValid get() {

    return INSTANCE;
  }
}
