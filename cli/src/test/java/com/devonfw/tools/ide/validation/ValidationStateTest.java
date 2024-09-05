package com.devonfw.tools.ide.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationStateTest {

  @Test
  void testAddTwoResults() {

    ValidationState stateOne = new ValidationState();
    stateOne.addErrorMessage("state one: first error message");
    stateOne.addErrorMessage("state one: second error message");
    ValidationState stateTwo = new ValidationState();
    stateTwo.addErrorMessage("state two: first error message");
    stateTwo.addErrorMessage("state two: second error message");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo("state one: first error message" + "\n"
        + "state one: second error message" + "\n"
        + "state two: first error message" + "\n"
        + "state two: second error message");
  }

  @Test
  void testAddEmptyResult() {

    ValidationState stateOne = new ValidationState();
    stateOne.addErrorMessage("state one: first error message");
    stateOne.addErrorMessage("state one: second error message");
    ValidationState stateTwo = new ValidationState();

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo("state one: first error message" + "\n" + "state one: second error message");
  }

  @Test
  void testAddToEmptyResult() {
    ValidationState stateOne = new ValidationState();
    ValidationState stateTwo = new ValidationState();
    stateTwo.addErrorMessage("state two: first error message");
    stateTwo.addErrorMessage("state two: second error message");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo("state two: first error message" + "\n" + "state two: second error message");
  }

  @Test
  void testAddBothEmptyResult() {
    ValidationState stateOne = new ValidationState();
    ValidationState stateTwo = new ValidationState();

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isNull();
  }
}
