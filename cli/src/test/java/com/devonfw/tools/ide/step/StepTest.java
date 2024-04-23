package com.devonfw.tools.ide.step;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

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
      step.end();
    }
    // assert
    assertThat(step.getSuccess()).isTrue();
    assertThat(step.getDuration()).isPositive();
    assertLogMessage(context, IdeLogLevel.TRACE, "Starting step Test-Step...");
    assertLogMessage(context, IdeLogLevel.STEP, "Start: Test-Step");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "The Test-Step succeeded as expected");
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended successfully.");
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
      step.end();
    }
    // assert
    assertThat(step.getSuccess()).isTrue();
    assertThat(step.getDuration()).isPositive();
    assertThat(step.getParameterCount()).isEqualTo(2);
    assertThat(step.getParameter(0)).isEqualTo("arg1");
    assertThat(step.getParameter(1)).isEqualTo("arg2");
    assertThat(step.getParameter(2)).isNull();
    assertLogMessage(context, IdeLogLevel.TRACE, "Starting step Test-Step with params [arg1, arg2]...");
    assertNoLogMessage(context, IdeLogLevel.STEP, "Start: Test-Step");
    assertNoLogMessage(context, IdeLogLevel.SUCCESS, "Test-Step", true);
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended successfully.");
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
      step.end();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertLogMessage(context, IdeLogLevel.TRACE, "Starting step Test-Step...");
    assertLogMessage(context, IdeLogLevel.STEP, "Start: Test-Step");
    assertLogMessage(context, IdeLogLevel.ERROR, "The Test-Step failed as expected");
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended with failure.");
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
      step.end();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertLogMessage(context, IdeLogLevel.TRACE, "Starting step Test-Step...");
    assertLogMessage(context, IdeLogLevel.STEP, "Start: Test-Step");
    assertLogMessage(context, IdeLogLevel.WARNING,
        "Step 'Test-Step' already ended with true and now ended again with false.");
    assertLogMessage(context, IdeLogLevel.ERROR, "unexpected situation!");
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended with failure.");
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
      step.end();
    }
    assertThat(step.getSuccess()).isFalse();
    assertThat(step.getDuration()).isPositive();
    // assert
    assertLogMessage(context, IdeLogLevel.TRACE, "Starting step Test-Step...");
    assertLogMessage(context, IdeLogLevel.STEP, "Start: Test-Step");
    assertLogMessage(context, IdeLogLevel.ERROR, "The Test-Step failed as expected");
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended with failure.");
    assertLogMessage(context, IdeLogLevel.WARNING,
        "Step 'Test-Step' already ended with false and now ended again with true.");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "The Test-Step succeeded as expected");
    assertLogMessage(context, IdeLogLevel.DEBUG, "Step 'Test-Step' ended successfully.");
  }

}
