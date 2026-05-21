package com.devonfw.ide.gui.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarNone;
import com.devonfw.tools.ide.process.OutputListener;
import com.devonfw.tools.ide.process.ProcessContext;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashbaord (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {

  private OutputListener outputListener;

  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeGuiContext(IdeStartContextImpl startContext, Path workingDirectory) {

    super(startContext, workingDirectory);
  }

  //TODO: UPDATE THIS
  @Override
  protected String readLine() {

    return "yes";
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {

    return new IdeProgressBarNone(title, 0, unitName, unitSize);
  }

  /**
   * Sets the output listener for process output.
   *
   * @param outputListener the output listener
   */
  public void setOutputListener(OutputListener outputListener) {
    this.outputListener = outputListener;
  }

  /**
   * @return the output listener
   */
  public OutputListener getOutputListener() {
    return this.outputListener;
  }

  @Override
  public ProcessContext newProcess() {
    ProcessContext processContext = super.newProcess();
    if (this.outputListener != null) {
      processContext.setOutputListener(this.outputListener);
    }
    return processContext;
  }
}
