package com.devonfw.ide.gui.progress;

/**
 * The outcome tally of the sub-tasks below a {@link GuiTask}, rendered as chips in the task overview.
 *
 * @param running the number of sub-tasks that are still running.
 * @param succeeded the number of sub-tasks that ended successfully.
 * @param failed the number of sub-tasks that ended without success.
 */
public record TaskStats(int running, int succeeded, int failed) {

  /** Tally of a task that has no sub-tasks at all. */
  public static final TaskStats NONE = new TaskStats(0, 0, 0);

  /**
   * @return the total number of sub-tasks seen so far.
   */
  public int total() {

    return this.running + this.succeeded + this.failed;
  }

  /**
   * @return {@code true} if there is nothing to report, {@code false} otherwise.
   */
  public boolean isEmpty() {

    return total() == 0;
  }
}
