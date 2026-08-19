package com.devonfw.ide.gui.progress.step;

import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.step.StepImpl;

/// Implementation of {@link Step} for the GUI.
public class GuiStep extends StepImpl {

  private final TaskManager taskManager;

  /**
   * Creates and starts a new {@link StepImpl}.
   *
   * @param context the {@link IdeContext}.
   * @param parent the {@link #getParent() parent step}.
   * @param name the {@link #getName() step name}.
   * @param silent the {@link #isSilent() silent flag}.
   * @param params the parameters. Should have reasonable {@link Object#toString() string representations}.
   */
  public GuiStep(TaskManager taskManager, AbstractIdeContext context, StepImpl parent, String name, boolean silent, Object... params) {

    this.taskManager = taskManager;
    super(context, parent, name, silent, params);
  }

  @Override
  public void close() {

    taskManager.removeStep(this);
    super.close();
  }
}
