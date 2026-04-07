package com.devonfw.ide.gui.progress;

import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

public class GuiProgressBar extends AbstractIdeProgressBar {

  public GuiProgressBar(String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
  }


  protected void doStepBy(long stepSize, long currentProgress) {
    // TODO Auto-generated method stub

  }

  protected void doStepTo(long stepPosition) {
    // TODO Auto-generated method stub

  }
}
