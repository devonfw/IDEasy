package com.devonfw.ide.gui.progress;

import java.util.logging.Logger;

import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

/**
 * This is a handler for the progress bars in the GUI
 */
public class GuiProgressBarHandling extends AbstractIdeProgressBar {

  private static final Logger LOG = Logger.getLogger(GuiProgressBarHandling.class.getName());

  private boolean isIndeterminate = false;

  public GuiProgressBarHandling(String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    TaskManager.getInstance().addTask(this);
  }

  /**
   * This constructor is used for indeterminate progress bars in the UI.
   *
   * @param title the title of the progress bar
   */
  public GuiProgressBarHandling(String title) {

    super(title, 100, "%", 1);
    this.isIndeterminate = true;
    TaskManager.getInstance().addTask(this);
  }


  protected void doStepBy(long stepSize, long currentProgress) {
    // TODO review if there is a better way to implement this

    TaskManager.getInstance().updateTask(this, currentProgress + stepSize);
  }

  protected void doStepTo(long stepPosition) {
    // TODO review if there is a better way to implement this

    LOG.info("Step to " + stepPosition);
    TaskManager.getInstance().updateTask(this, stepPosition);
  }

  @Override
  public void close() {

    LOG.info("Closing progress bar");
    TaskManager.getInstance().removeTask(this);
    super.close();
  }

  /**
   * @return true if the progress bar is indeterminate, false otherwise
   */
  public boolean isIndeterminate() {
    return isIndeterminate;
  }

  /**
   * @param indeterminate set whether the progress bar is indeterminate or not
   */
  public void setIndeterminate(boolean indeterminate) {
    isIndeterminate = indeterminate;
  }
}
