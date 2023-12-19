package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;

import java.util.ArrayList;
import java.util.List;

public class StepContainer {

  private final IdeContext context;

  private List<String> successfulSteps;
  private List<String> failedSteps;

  public StepContainer(IdeContext context) {

    this.context = context;
    successfulSteps = new ArrayList<>();
    failedSteps = new ArrayList<>();
  }

  public void startStep(String stepName) {

    this.context.step("Starting step: {}", stepName);
  }

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
