package com.devonfw.ide.gui.context;

import java.nio.file.Path;
import java.util.UUID;

import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.step.GuiStep;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.step.StepImpl;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashboard (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {

  private final TaskManager taskManager;

  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   * @param taskManager the {@link TaskManager} to manage tasks and progress bars in the GUI.
   */
  public IdeGuiContext(IdeStartContextImpl startContext, Path workingDirectory, TaskManager taskManager) {

    super(startContext, workingDirectory);
    this.taskManager = taskManager;
  }

  @Override
  protected String readLine() {

    return "";
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {

    ProgressBarTask newTask = new ProgressBarTask(taskManager, UUID.randomUUID().toString(), title, size, unitName, unitSize);
    taskManager.addTask(newTask);

    return newTask;
  }

  /**
   * @param title the title of the progress bar
   * @return a progress bar implementation that is indeterminate
   */
  public IdeProgressBar newProgressBarIndeterminate(String title) {

    ProgressBarTask newTask = new ProgressBarTask(taskManager, UUID.randomUUID().toString(), title);
    taskManager.addTask(newTask);

    return newTask;
  }

  @Override
  public Step newStep(String name) {
    GuiStep step = new GuiStep(taskManager, this, null, name, false);
    taskManager.addStep(step);

    return step;
  }

  @Override
  public Step newStep(String name, Object... parameters) {
    GuiStep step = new GuiStep(taskManager, this, null, name, false, parameters);
    taskManager.addStep(step);

    return step;
  }

  @Override
  public StepImpl newStep(boolean silent, String name, Object... parameters) {
    GuiStep step = new GuiStep(taskManager, this, null, name, silent, parameters);
    taskManager.addStep(step);

    return step;
  }
}
