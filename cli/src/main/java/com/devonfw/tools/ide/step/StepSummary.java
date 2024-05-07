package com.devonfw.tools.ide.step;

/**
 * Simple container for the overall summary of a {@link Step}.
 *
 * @see StepImpl#logSummary(boolean)
 */
class StepSummary {

  private int total;

  private int error;

  /**
   * @return the total number of {@link Step}s that had been executed.
   */
  public int getTotal() {

    return this.total;
  }

  /**
   * @return the number of {@link Step}s that failed.
   */
  public int getError() {

    return this.error;
  }

  /**
   * @param failure - see {@link Step#isFailure()}.
   */
  public void add(boolean failure) {

    this.total++;
    if (failure) {
      this.error++;
    }
  }

  @Override
  public String toString() {

    return this.error + " step(s) failed out of " + this.total + " steps.";
  }

}
