package com.devonfw.tools.ide.validation;

public class ValidationState implements ValidationResult {

  private StringBuilder errorMessage;

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
      this.errorMessage = new StringBuilder(error.length());
    } else {
      this.errorMessage.append('\n');
    }
    this.errorMessage.append(error);
  }

  public void add(ValidationResult result) {
    if (!result.isValid()) {
      addErrorMessage(result.getErrorMessage());
    }
  }
}
