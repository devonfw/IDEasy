package com.devonfw.ide.gui.progress;

/**
 * The lifecycle state of a {@link GuiTask}.
 */
public enum TaskState {

  /** The task is still in progress. */
  RUNNING,

  /** The task completed successfully. */
  SUCCESS,

  /**
   * The task ended without success. Note that a {@link com.devonfw.tools.ide.step.Step} that is closed without an explicit outcome is recorded as a failure by
   * {@link com.devonfw.tools.ide.step.StepImpl}, so this state is also reached when a step was simply never completed.
   */
  FAILED;

  /**
   * @return {@code true} if this state is terminal (the task will not change anymore), {@code false} for {@link #RUNNING}.
   */
  public boolean isTerminal() {

    return this != RUNNING;
  }
}
