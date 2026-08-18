package com.devonfw.ide.gui.progress;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.tools.ide.io.AbstractIdeProgressBar;

/**
 * This is a handler for the progress bars in the GUI.
 * <p>
 * A progress bar is a quantitative, short-lived {@link GuiTask}: it removes itself from the {@link TaskManager} as soon as it is {@link #close() closed}, so it
 * is never {@link #isDismissable() dismissable}.
 */
public class ProgressBarTask extends AbstractIdeProgressBar implements GuiTask {

  /** Format of the {@link #detailProperty() detail}, following the scheme "[current/maximum unit]". */
  public static final String DETAIL_STRING_FORMAT = "[%d/%d %s]";

  private static final Logger LOG = LoggerFactory.getLogger(ProgressBarTask.class.getName());

  private final TaskManager taskManager;

  private final GuiTaskModel model;

  /** The raw progress in the unit reported by the CLI. Kept separate from {@link #progressProperty()}, which is the normalized fraction the UI renders. */
  private final LongProperty currentProgressProperty = new SimpleLongProperty(0);

  /** Guards against queueing more than one UI update at a time. @see #publishProgress(long) */
  private final AtomicBoolean updateScheduled = new AtomicBoolean();

  /** The most recent progress reported by the CLI, read by the scheduled UI update. */
  private volatile long pendingProgress;

  /**
   * @param taskManager the {@link TaskManager} to link this progress bar to. Note: The task manager supplied here is only used for closing the task, in
   *     case {link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title title of the task
   * @param maxSize maximum progress
   * @param unitName unit of the progress (e.g., %, MB, files, etc.)
   * @param unitSize unit size (e.g., 1%)
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title, long maxSize, String unitName, long unitSize) {

    super(title, maxSize, unitName, unitSize);
    this.taskManager = taskManager;
    this.model = new GuiTaskModel(taskId, title, isIndeterminate() ? GuiTaskModel.INDETERMINATE : 0.0);
    updateDetailText(0);
  }

  /**
   * This constructor is used for indeterminate progress bars in the UI.
   *
   * @param taskManager the {@link TaskManager} to link this progress bar to. Note: The task manager supplied here is only used for closing the task, in
   *     case {link #close()} is called.
   * @param taskId a unique id to identify this task.
   * @param title the title of the progress bar
   */
  public ProgressBarTask(TaskManager taskManager, String taskId, String title) {

    // a maximum size of -1 is how IdeProgressBar expresses "the maximum is undefined".
    this(taskManager, taskId, title, -1, "%", 1);
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

  /**
   * @return the subtitle, which is always empty: a progress bar has no sub-tasks to report on.
   */
  @Override
  public StringProperty subtitleProperty() {

    return this.model.subtitleProperty();
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

  /**
   * @return the stats, which stay {@link TaskStats#NONE}: a progress bar has no sub-tasks to tally.
   */
  @Override
  public ReadOnlyObjectProperty<TaskStats> statsProperty() {

    return this.model.statsProperty();
  }

  /**
   * @return {@code true} if the maximum size of this progress bar is undefined so that the progress cannot be quantified, {@code false} otherwise.
   */
  public boolean isIndeterminate() {

    return this.maxSize <= 0;
  }

  /**
   * Properties are relevant for dynamically updating the ui.
   *
   * @return the raw progress of this task in its {@link #getUnitName() unit}.
   */
  public LongProperty currentProgressProperty() {

    return this.currentProgressProperty;
  }

  // currentProgress is only for test purposes, see AbstractIdeProgressBar
  @Override
  protected void doStepBy(long stepSize, long currentProgress) {

    publishProgress(getCurrentProgress());
  }

  @Override
  protected void doStepTo(long stepPosition) {

    publishProgress(stepPosition);
  }

  /**
   * Hands the latest progress to the UI, coalescing bursts into at most one pending update.
   * <p>
   * Progress is reported far faster than a UI can render it - copying reads in 1 KiB chunks, so a large archive produces hundreds of thousands of calls.
   * Posting every one of them to the JavaFX Application Thread floods its event queue and freezes the UI. Instead only the latest value is kept and a single
   * update is scheduled; while it is outstanding, further calls just overwrite that value. The UI therefore refreshes as fast as it can drain, and no faster,
   * regardless of how quickly the background thread reports.
   *
   * @param progress the current progress.
   */
  private void publishProgress(long progress) {

    this.pendingProgress = progress;
    if (this.updateScheduled.compareAndSet(false, true)) {
      FxHelper.runFxSafe(this::applyPendingProgress);
    }
  }

  /**
   * Applies the latest progress to every value the UI renders. Runs on the JavaFX Application Thread and, thanks to the coalescing in
   * {@link #publishProgress(long)}, only as often as the UI can actually draw - so writing the values eagerly here costs nothing.
   * <p>
   * They are written rather than derived by a binding on purpose: a bound property signals its listeners only on the transition from valid to invalid, which
   * does not survive the round trip through the task list's extractor reliably. Writing all values within one FX turn keeps them consistent.
   */
  private void applyPendingProgress() {

    // Released before applying, so that a value arriving while we render schedules a fresh update instead of being dropped.
    this.updateScheduled.set(false);
    long progress = this.pendingProgress;
    LOG.debug("Updating progress bar {} to {}", getId(), progress);
    this.currentProgressProperty.set(progress);
    if (!isIndeterminate()) {
      this.model.setProgress((double) progress / this.maxSize);
    }
    updateDetailText(progress);
  }

  private void updateDetailText(long progress) {

    if (isIndeterminate()) {
      // there is no meaningful "x of y" to show - the animated bar carries the information that something is happening.
      this.model.setDetail("");
    } else {
      this.model.setDetail(String.format(DETAIL_STRING_FORMAT, toUnits(progress), toUnits(this.maxSize), getUnitName()));
    }
  }

  /**
   * @param rawProgress the progress as counted by the CLI (e.g. bytes).
   * @return the progress expressed in {@link #getUnitName() units} (e.g. MiB), so that the number matches the displayed unit.
   */
  private long toUnits(long rawProgress) {

    if (this.unitSize <= 1) {
      return rawProgress;
    }
    return rawProgress / this.unitSize;
  }

  @Override
  public void close() {

    LOG.info("Closing progress bar");
    this.model.setState(TaskState.SUCCESS);
    this.taskManager.removeTask(this);
    super.close();
  }
}
