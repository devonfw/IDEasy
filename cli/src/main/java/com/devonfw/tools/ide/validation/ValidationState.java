package com.devonfw.tools.ide.validation;

public class ValidationState implements ValidationResult {

  private StringBuilder errorMessage;

  private String propertyName;

  public ValidationState(String propertyName) {
    this.propertyName = propertyName;
  }

  public boolean isValid() {
    return (this.errorMessage == null);
  }

  public String getErrorMessage() {
    if (this.errorMessage == null) {
      return null;
    }
    return this.errorMessage.toString();
  }

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
