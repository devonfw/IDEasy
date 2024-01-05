package com.devonfw.tools.ide.common;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to manage and log the progress of steps in a process.
 * Each step can be started, ended with success or failure, and the overall completion
 * status can be checked.
 * @throws CliException if one or more steps fail.
 */
public class StepContainer {

  private final IdeContext context;

  /** List of steps that ended successfully. */
  private List<String> successfulSteps;

  /** List of steps that failed. */
  private List<String> failedSteps;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public StepContainer(IdeContext context) {

    this.context = context;
    successfulSteps = new ArrayList<>();
    failedSteps = new ArrayList<>();
  }

  /**
   * Logs the start of a step.
   *
   * @param stepName the name of the step.
   */
  public void startStep(String stepName) {

    this.context.step("Starting step: {}", stepName);
  }

  /**
   * Logs the end of a step, indicating success or failure.
   *
   * @param stepName the name of the step.
   * @param success {@code true} if the step succeeded, {@code false} otherwise.
   * @param e the exception associated with the failure, or {@code null} if the step succeeded.
   */
  public void endStep(String stepName, boolean success, Throwable e) {

    if (success) {
      successfulSteps.add(stepName);
      this.context.success("Step '{}' succeeded.", stepName);
    } else {
      failedSteps.add(stepName);
      this.context.warning("Step '{}' failed.", stepName);
      if (e != null) {
        this.context.error(e);
      }
    }
  }

  /**
   * Checks the overall completion status of all steps.
   *
   * @throws CliException if one or more steps fail, providing a detailed summary.
   */
  public void complete() {

    if (failedSteps.isEmpty()) {
      this.context.success("All {} steps ended successfully!", successfulSteps.size());
    } else {
      throw new CliException(String.format("%d step(s) failed (%d%%) and %d step(s) succeeded (%d%%) out of %d step(s)!",
          failedSteps.size(), calculatePercentage(failedSteps.size()), successfulSteps.size(),
          100 - calculatePercentage(failedSteps.size()), successfulSteps.size() + failedSteps.size()));
    }
  }

  private int calculatePercentage(int count) {

    return (count * 100) / (successfulSteps.size() + failedSteps.size());
  }

}
