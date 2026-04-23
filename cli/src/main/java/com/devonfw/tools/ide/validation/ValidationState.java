package com.devonfw.tools.ide.validation;

import com.devonfw.tools.ide.cli.CliArgument;

/**
 * Implementation of {@link ValidationResult} as a mutable state that can collect errors dynamically.
 */
public class ValidationState implements ValidationResult {

  private final String propertyName;

  private StringBuilder errorMessage;

  /**
   * Field for the {@link CliArgument} that was the reason for a failed validation.
   */
  private CliArgument cliArgument;

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

  private String parseHint;

  private String parseExceptionMessage;

  /**
   * @param cliArgument The {@link CliArgument} that failed the validation.
   */
  public void setCliArgument(CliArgument cliArgument) {
    this.cliArgument = cliArgument;
  }

  /**
   * @return The {@link CliArgument} that failed the validation.
   */
  public CliArgument getCliArgument() {
    return this.cliArgument;
  }

  /**
   * @param parseHint the hint to display when a parse error occurred (e.g. a list of valid values).
   */
  public void setParseHint(String parseHint) {
    this.parseHint = parseHint;
  }

  /**
   * @return the hint for the failed parse, or {@code null} if none.
   */
  public String getParseHint() {
    return this.parseHint;
  }

  /**
   * @param parseExceptionMessage the message from a non-{@link IllegalArgumentException} parse failure.
   */
  public void setParseExceptionMessage(String parseExceptionMessage) {
    this.parseExceptionMessage = parseExceptionMessage;
  }

  /**
   * @return the exception message from a non-{@link IllegalArgumentException} parse failure, or {@code null}.
   */
  public String getParseExceptionMessage() {
    return this.parseExceptionMessage;
  }
}
