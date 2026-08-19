package com.devonfw.ide.gui.context;

import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.progress.ProgressBarTask;
import com.devonfw.ide.gui.progress.step.GuiStep;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.step.StepImpl;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashboard (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {

  private static final Logger LOG = LoggerFactory.getLogger(IdeGuiContext.class);

  private final TaskManager taskManager;

  /**
   * The innermost currently running {@link GuiStep}, or {@code null} if no step is running. The GUI creates one context per command execution, so the currently
   * running step is never shared between concurrently running commands.
   *
   * @see GuiStateManager#newRunContext()
   */
  private GuiStep currentGuiStep;

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

    ProgressBarTask newTask = new ProgressBarTask(this.taskManager, UUID.randomUUID().toString(), title, size, unitName, unitSize);
    this.taskManager.addTask(newTask);

    return newTask;
  }

  @Override
  public GuiStep getCurrentStep() {

    return this.currentGuiStep;
  }

  @Override
  public GuiStep newStep(boolean silent, String name, Object... parameters) {

    GuiStep parent = this.currentGuiStep;
    GuiStep step = new GuiStep(this, parent, name, silent, parameters);
    this.currentGuiStep = step;
    if (parent == null) {
      // only root steps become their own task, nested steps feed the report of their root.
      this.taskManager.addTask(step);
    } else {
      // every descendant resolves the same root, so grandchildren land in the root's flat list rather than nesting.
      step.getRoot().recordChildStart(step);
    }
    updateRootSubtitle();
    return step;
  }

  /**
   * Names the innermost running step as the subtitle of its root, so the user sees what the task is doing right now. Only this class can do it, because the
   * step stack is what identifies the innermost step - a step itself knows its parent but not which of its descendants is currently active.
   */
  private void updateRootSubtitle() {

    GuiStep current = this.currentGuiStep;
    if (current == null) {
      return; // the root step ended and cleared its own subtitle.
    }
    GuiStep root = current.getRoot();
    root.setSubtitle((current == root) ? "" : current.getName());
  }

  @Override
  public void endStep(StepImpl step) {

    if (!(step instanceof GuiStep guiStep)) {
      super.endStep(step);
      return;
    }
    guiStep.onEnd();
    if (guiStep == this.currentGuiStep) {
      this.currentGuiStep = guiStep.getGuiParentStep();
      updateRootSubtitle();
    } else {
      String currentStepName = (this.currentGuiStep == null) ? "null" : this.currentGuiStep.getName();
      LOG.warn("endStep called with wrong step '{}' but expected '{}'", step.getName(), currentStepName);
    }
  }
}
