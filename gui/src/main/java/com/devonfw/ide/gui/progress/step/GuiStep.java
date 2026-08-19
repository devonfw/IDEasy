package com.devonfw.ide.gui.progress.step;

import java.util.UUID;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.progress.GuiTask;
import com.devonfw.ide.gui.progress.GuiTaskModel;
import com.devonfw.ide.gui.progress.TaskState;
import com.devonfw.ide.gui.progress.TaskStats;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.step.StepImpl;

/**
 * Implementation of {@link Step} for the GUI, displayed as a {@link GuiTask}.
 * <p>
 * Unlike a {@link com.devonfw.ide.gui.progress.ProgressBarTask}, a step carries an outcome that the end-user needs to see, so it is <em>not</em> removed from
 * the {@link com.devonfw.ide.gui.context.TaskManager} when it ends. It stays until the user dismisses it.
 * <p>
 * Only root steps become their own task; nested steps instead contribute to the report of their {@link #getRoot() root}. The counters below are therefore only
 * meaningful on a root step.
 */
public class GuiStep extends StepImpl implements GuiTask {

  private final GuiStep guiParentStep;

  private final GuiTaskModel model;

  private int childrenRunning;

  private int childrenSucceeded;

  private int childrenFailed;

  /**
   * All descendants of this root, flattened to a single level and in the order they started. Like the counters above this is only populated on a root step.
   * Append-only - a sub-step stays here once it has ended, because its outcome is exactly what the user expands the task to see.
   */
  private final ObservableList<GuiStep> subSteps = FXCollections.observableArrayList();

  private final ObservableList<GuiStep> subStepsReadOnly = FXCollections.unmodifiableObservableList(this.subSteps);

  /**
   * Creates and starts a new {@link GuiStep}.
   *
   * @param context the {@link IdeContext}.
   * @param parent the {@link #getParent() parent step} or {@code null} for a root step.
   * @param name the {@link #getName() step name}.
   * @param silent the {@link #isSilent() silent flag}.
   * @param params the parameters. Should have reasonable {@link Object#toString() string representations}.
   */
  public GuiStep(AbstractIdeContext context, GuiStep parent, String name, boolean silent, Object... params) {

    super(context, parent, name, silent, params);
    this.guiParentStep = parent;
    // a step has no quantifiable progress - it is either running or it has ended.
    this.model = new GuiTaskModel(UUID.randomUUID().toString(), name, GuiTaskModel.INDETERMINATE);
  }

  @Override
  public String getId() {

    return this.model.getId();
  }

  @Override
  public StringProperty titleProperty() {

    return this.model.titleProperty();
  }

  @Override
  public StringProperty detailProperty() {

    return this.model.detailProperty();
  }

  @Override
  public StringProperty subtitleProperty() {

    return this.model.subtitleProperty();
  }

  /**
   * @param subtitle the name of the sub-step that is currently running below this root step, or the empty string if there is none.
   */
  public void setSubtitle(String subtitle) {

    this.model.setSubtitle(subtitle);
  }

  @Override
  public StringExpression displayTextProperty() {

    return this.model.displayTextProperty();
  }

  @Override
  public ReadOnlyDoubleProperty progressProperty() {

    return this.model.progressProperty();
  }

  @Override
  public ReadOnlyObjectProperty<TaskState> stateProperty() {

    return this.model.stateProperty();
  }

  @Override
  public ReadOnlyObjectProperty<TaskStats> statsProperty() {

    return this.model.statsProperty();
  }

  @Override
  public boolean isDismissable() {

    return true;
  }

  /**
   * @return the {@link #getParent() parent} as {@link GuiStep} or {@code null} if this is a root step.
   */
  public GuiStep getGuiParentStep() {

    return this.guiParentStep;
  }

  /**
   * @return the top-most {@link GuiStep} of this step hierarchy, which is the one displayed as a task. Returns {@code this} for a root step.
   */
  public GuiStep getRoot() {

    GuiStep root = this;
    while (root.guiParentStep != null) {
      root = root.guiParentStep;
    }
    return root;
  }

  @Override
  public ObservableList<GuiStep> getSubTasks() {

    return this.subStepsReadOnly;
  }

  /**
   * Records that a nested step below this root has been started.
   *
   * @param child the nested step. Appended to {@link #getSubTasks()}, which is what puts the most recently started step at the bottom of the list.
   */
  public synchronized void recordChildStart(GuiStep child) {

    this.childrenRunning++;
    FxHelper.runFxSafe(() -> this.subSteps.add(child));
    updateStepStats();
  }

  /**
   * Records that a nested step below this root has ended.
   *
   * @param success {@code true} if the nested step {@link #isSuccess() succeeded}, {@code false} otherwise.
   */
  public synchronized void recordChildEnd(boolean success) {

    this.childrenRunning--;
    if (success) {
      this.childrenSucceeded++;
    } else {
      this.childrenFailed++;
    }
    updateStepStats();
  }

  /**
   * Called exactly once when this step has ended, from {@link com.devonfw.ide.gui.context.IdeGuiContext#endStep(StepImpl)}. That is the only reliable hook: a
   * step may end via {@link #success()}, {@link #error(Throwable)} or {@link #close()}, and {@link StepImpl} notifies the context on whichever path ended it,
   * after the outcome has been recorded.
   */
  public void onEnd() {

    boolean success = isSuccess();
    this.model.setProgress(GuiTaskModel.COMPLETE);
    this.model.setState(success ? TaskState.SUCCESS : TaskState.FAILED);
    GuiStep root = getRoot();
    if (root == this) {
      // the whole task is done, so there is no sub-step left to name.
      this.model.setSubtitle("");
      updateStepStats();
    } else {
      root.recordChildEnd(success);
    }
  }

  private synchronized void updateStepStats() {

    this.model.setStats(new TaskStats(this.childrenRunning, this.childrenSucceeded, this.childrenFailed));
  }
}
