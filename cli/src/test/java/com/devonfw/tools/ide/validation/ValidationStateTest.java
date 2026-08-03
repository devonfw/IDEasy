package com.devonfw.tools.ide.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link ValidationState}.
 */
class ValidationStateTest {

  @Test
  void testAddTwoResults() {

    ValidationState stateOne = new ValidationState("testPropertyOne");
    stateOne.addErrorMessage("state one: first error message");
    stateOne.addErrorMessage("state one: second error message");
    ValidationState stateTwo = new ValidationState("testPropertyTwo");
    stateTwo.addErrorMessage("state two: first error message");
    stateTwo.addErrorMessage("state two: second error message");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo(
        "Error in property testPropertyOne:" + "\n"
            + "state one: first error message" + "\n"
            + "state one: second error message" + "\n"
            + "Error in property testPropertyTwo:" + "\n"
            + "state two: first error message" + "\n"
            + "state two: second error message");
  }

  @Test
  void testAddEmptyResult() {

    ValidationState stateOne = new ValidationState("testPropertyOne");
    stateOne.addErrorMessage("state one: first error message");
    stateOne.addErrorMessage("state one: second error message");
    ValidationState stateTwo = new ValidationState("testPropertyTwo");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo(
        "Error in property testPropertyOne:" + "\n"
            + "state one: first error message" + "\n"
            + "state one: second error message");
  }

  @Test
  void testAddToEmptyResult() {
    ValidationState stateOne = new ValidationState("testPropertyOne");
    ValidationState stateTwo = new ValidationState("testPropertyTwo");
    stateTwo.addErrorMessage("state two: first error message");
    stateTwo.addErrorMessage("state two: second error message");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isEqualTo(
        "Error in property testPropertyTwo:" + "\n"
            + "state two: first error message" + "\n"
            + "state two: second error message");
  }

  @Test
  void testAddBothEmptyResult() {
    ValidationState stateOne = new ValidationState("testPropertyOne");
    ValidationState stateTwo = new ValidationState("testPropertyTwo");

    stateOne.add(stateTwo);

    assertThat(stateOne.getErrorMessage()).isNull();
  }
}
