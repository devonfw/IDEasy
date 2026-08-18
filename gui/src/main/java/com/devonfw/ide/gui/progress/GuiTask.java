package com.devonfw.ide.gui.progress;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 * A unit of work that is displayed to the end-user in the status bar and in the task overview window.
 * <p>
 * This is the single abstraction the UI layer renders. It is implemented both by {@link ProgressBarTask} (an
 * {@link com.devonfw.tools.ide.io.IdeProgressBar}) and by {@link com.devonfw.ide.gui.progress.step.GuiStep} (a
 * {@link com.devonfw.tools.ide.step.Step}). Those two extend different CLI classes and can therefore never share a common base class, so the shared state lives
 * in a {@link GuiTaskModel} that both of them own by composition and delegate to.
 */
public interface GuiTask {

  /**
   * @return the unique id of this task. Used to distinguish multiple tasks that share the same {@link #titleProperty() title}.
   */
  String getId();

  /**
   * @return the title of this task, e.g. "Downloading" or the name of a {@link com.devonfw.tools.ide.step.Step}.
   */
  ReadOnlyStringProperty titleProperty();

  /**
   * @return the additional detail rendered next to the {@link #titleProperty() title}, e.g. "[12/40 MiB]" for a progress bar or "2 of 5 sub-steps failed" for a
   *     step. Empty if there is nothing to add.
   */
  ReadOnlyStringProperty detailProperty();

  /**
   * @return the secondary line rendered below the title, naming what the task is doing right now (for a step: the innermost running sub-step). Empty if the
   *     task has nothing more specific to report.
   */
  ReadOnlyStringProperty subtitleProperty();

  /**
   * @return the {@link #titleProperty() title} and the {@link #detailProperty() detail} as a single string to display.
   */
  StringExpression displayTextProperty();

  /**
   * @return the progress as a fraction between {@code 0.0} and {@code 1.0}, or {@link GuiTaskModel#INDETERMINATE} if the progress cannot be quantified. The
   *     value maps directly onto {@link javafx.scene.control.ProgressBar#progressProperty()}.
   */
  ReadOnlyDoubleProperty progressProperty();

  /**
   * @return the current {@link TaskState}.
   */
  ReadOnlyObjectProperty<TaskState> stateProperty();

  /**
   * @return the outcome tally of the sub-tasks below this task, rendered as chips. {@link TaskStats#NONE} for a task that has no sub-tasks.
   *
   * @see com.devonfw.ide.gui.progress.step.GuiStep
   */
  ReadOnlyObjectProperty<TaskStats> statsProperty();

  /**
   * @return {@code true} if the end-user may remove this task from the task list once it is {@link TaskState#isTerminal() finished}, {@code false} if it
   *     disappears on its own.
   */
  default boolean isDismissable() {

    return false;
  }

  /**
   * @return the current {@link TaskState}.
   */
  default TaskState getState() {

    return stateProperty().get();
  }

  /**
   * @return {@code true} if this task is still {@link TaskState#RUNNING running}, {@code false} otherwise.
   */
  default boolean isRunning() {

    return getState() == TaskState.RUNNING;
  }
}
