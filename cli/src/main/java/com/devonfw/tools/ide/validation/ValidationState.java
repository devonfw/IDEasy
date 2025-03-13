package com.devonfw.tools.ide.validation;

/**
 * Implementation of {@link ValidationResult} as a mutable state that can collect errors dynamically.
 */
public class ValidationState implements ValidationResult {

  private final String propertyName;

  private StringBuilder errorMessage;

  /**
   * The default constructor for no property.
   */
  public ValidationState() {
    this(null);
  }

  /**
   * @param propertyName the name of the property to validate.
   */
  public ValidationState(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public boolean isValid() {
    return (this.errorMessage == null);
  }

  @Override
  public String getErrorMessage() {
    if (this.errorMessage == null) {
      return null;
    }
    return this.errorMessage.toString();
  }

  /**
   * @param error the error message to add to this {@link ValidationState}.
   */
  public void addErrorMessage(String error) {
    if (this.errorMessage == null) {
      if (this.propertyName == null) {
        this.errorMessage = new StringBuilder(error.length() + 1);
        this.errorMessage.append('\n');
      } else {
        this.errorMessage = new StringBuilder(error.length() + propertyName.length() + 21); // 21 for the static text below
        this.errorMessage.append(String.format("Error in property %s:", propertyName));
        this.errorMessage.append('\n');
      }
    } else {
      this.errorMessage.append('\n');
    }
    this.errorMessage.append(error);
  }

  /**
   * @param result the {@link ValidationResult} to add to this {@link ValidationState}.
   */
  public void add(ValidationResult result) {
    if (!result.isValid()) {
      if (this.errorMessage == null) {
        this.errorMessage = new StringBuilder(result.getErrorMessage().length());
        this.errorMessage.append(result.getErrorMessage());
      } else {
        addErrorMessage(result.getErrorMessage());
      }
    }
  }
}
