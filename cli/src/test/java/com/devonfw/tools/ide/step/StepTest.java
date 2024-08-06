package com.devonfw.tools.ide.step;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link Step}.
 */
public class StepTest extends AbstractIdeContextTest {

  @Test
  public void testValidUsageSuccess() {

    // arrage
    IdeTestContext context = newContext(PROJECT_BASIC, "project", false);
    // act
    Step step = context.newStep("Test-Step");
    try {
      step.success("The Test-Step succeeded as expected");
    } finally {
      step.close();
    }
    // assert
    assertThat(step.getSuccess()).isTrue();
    assertThat(step.getDuration()).isPositive();
    assertThat(context).log().hasEntries(IdeLogEntry.ofTrace("Starting step Test-Step..."),
        IdeLogEntry.ofStep("Start: Test-Step"),
        IdeLogEntry.ofSuccess("The Test-Step succeeded as expected"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended successfully."));
  }

  @Test
  public void testValidUsageSuccessSilent() {

    // arrage
    IdeTestContext context = newContext(PROJECT_BASIC, "project", false);
    // act
    Step step = context.newStep(true, "Test-Step", "arg1", "arg2");
    try {
      step.success();
    } finally {
      step.close();
    }
    // assert
    assertThat(step.getSuccess()).isTrue();
    assertThat(step.getDuration()).isPositive();
    assertThat(step.getParameterCount()).isEqualTo(2);
    assertThat(step.getParameter(0)).isEqualTo("arg1");
    assertThat(step.getParameter(1)).isEqualTo("arg2");
    assertThat(step.getParameter(2)).isNull();
    assertThat(context).log().hasEntries(IdeLogEntry.ofTrace("Starting step Test-Step with params [arg1, arg2]..."),
        IdeLogEntry.ofStep("Start: Test-Step"),
        IdeLogEntry.ofSuccess("Test-Step"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended successfully."));
  }

  @Test
  public void testValidUsageError() {

    // arrage
    IdeTestContext context = newContext(PROJECT_BASIC, "project", false);
    // act
    Step step = context.newStep("Test-Step");
    try {
      step.error("The Test-Step failed as expected");
    } finally {
      step.close();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofTrace("Starting step Test-Step..."),
        IdeLogEntry.ofStep("Start: Test-Step"),
        IdeLogEntry.ofError("The Test-Step failed as expected"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended with failure."));
  }

  @Test
  public void testInvalidUsageSuccessError() {

    // arrage
    IdeTestContext context = newContext(PROJECT_BASIC, "project", false);
    // act
    Step step = context.newStep("Test-Step");
    try {
      step.success("The Test-Step succeeded as expected");
      throw new IllegalStateException("unexpected situation!");
    } catch (IllegalStateException e) {
      step.error(e);
    } finally {
      step.close();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofTrace("Starting step Test-Step..."),
        IdeLogEntry.ofStep("Start: Test-Step"),
        IdeLogEntry.ofWarning("Step 'Test-Step' already ended with true and now ended again with false."),
        IdeLogEntry.ofError("unexpected situation!"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended with failure."));
  }

  @Test
  public void testInvalidUsageErrorSuccess() {

    // arrage
    IdeTestContext context = newContext(PROJECT_BASIC, "project", false);
    // act
    Step step = context.newStep("Test-Step");
    try {
      step.error("The Test-Step failed as expected");
      // WOW this is really inconsistent and hopefully never happens elsewhere
      step.success("The Test-Step succeeded as expected");
    } finally {
      step.close();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertThat(context).log().hasEntries(IdeLogEntry.ofTrace("Starting step Test-Step..."),
        IdeLogEntry.ofStep("Start: Test-Step"),
        IdeLogEntry.ofError("The Test-Step failed as expected"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended with failure."),
        IdeLogEntry.ofWarning("Step 'Test-Step' already ended with false and now ended again with true."),
        IdeLogEntry.ofSuccess("The Test-Step succeeded as expected"),
        IdeLogEntry.ofDebug("Step 'Test-Step' ended successfully."));
  }

}
