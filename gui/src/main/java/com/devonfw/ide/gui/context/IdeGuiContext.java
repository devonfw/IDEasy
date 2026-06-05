package com.devonfw.ide.gui.context;

import java.nio.file.Path;
import java.util.UUID;

import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashbaord (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {

  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeGuiContext(IdeStartContextImpl startContext, Path workingDirectory) {

    super(startContext, workingDirectory);
  }

  @Override
  protected String readLine() {

    return "";
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {

    ProgressBarTask newTask = new ProgressBarTask(UUID.randomUUID().toString(), title, size, unitName, unitSize);
    TaskManager.getInstance().addTask(newTask);

    return newTask;
  }

  /**
   * @param title the title of the progress bar
   * @return a progress bar implementation that is indeterminate
   */
  public IdeProgressBar newProgressBarIndeterminate(String title) {

    ProgressBarTask newTask = new ProgressBarTask(UUID.randomUUID().toString(), title);
    TaskManager.getInstance().addTask(newTask);

    return newTask;
  }
}
