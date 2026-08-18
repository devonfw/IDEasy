package com.devonfw.ide.gui.progress;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.devonfw.ide.gui.FxHelper;
import com.devonfw.ide.gui.progress.step.GuiStep;

/**
 * Holds the observable state shared by every {@link GuiTask}.
 * <p>
 * {@link ProgressBarTask} and {@link GuiStep} must extend different CLI classes, so they cannot inherit this state from a common base class. Instead both own
 * an instance of this model and delegate their {@link GuiTask} methods to it. All mutations are routed through {@link FxHelper#runFxSafe(Runnable)} because
 * tasks are progressed from background threads while the properties are observed by the UI.
 */
public class GuiTaskModel {

  /** Value of {@link #progressProperty()} for a task whose progress cannot be quantified. */
  public static final double INDETERMINATE = -1.0;

  /** Value of {@link #progressProperty()} for a completed task. */
  public static final double COMPLETE = 1.0;

  private final String id;

  private final StringProperty title;

  private final StringProperty detail;

  private final StringProperty subtitle;

  private final DoubleProperty progress;

  private final ObjectProperty<TaskState> state;

  private final ObjectProperty<TaskStats> stats;

  private final StringExpression displayText;

  /**
   * The constructor.
   *
   * @param id the {@link #getId() id}.
   * @param title the initial {@link #titleProperty() title}.
   * @param progress the initial {@link #progressProperty() progress}, e.g. {@link #INDETERMINATE}.
   */
  public GuiTaskModel(String id, String title, double progress) {

    super();
    this.id = Objects.requireNonNull(id, "id");
    this.title = new SimpleStringProperty(title);
    this.detail = new SimpleStringProperty("");
    this.subtitle = new SimpleStringProperty("");
    this.progress = new SimpleDoubleProperty(progress);
    this.state = new SimpleObjectProperty<>(TaskState.RUNNING);
    this.stats = new SimpleObjectProperty<>(TaskStats.NONE);
    // computed once and shared, so that both the status bar and the task overview render the task identically.
    this.displayText = Bindings.createStringBinding(this::getDisplayText, this.title, this.detail);
  }

  private String getDisplayText() {

    String currentTitle = this.title.get();
    String currentDetail = this.detail.get();
    if ((currentDetail == null) || currentDetail.isEmpty()) {
      return currentTitle;
    }
    return currentTitle + " " + currentDetail;
  }

  /**
   * @return the unique id of the task.
   */
  public String getId() {

    return this.id;
  }

  /**
   * @return the title property.
   */
  public StringProperty titleProperty() {

    return this.title;
  }

  /**
   * @return the detail property.
   */
  public StringProperty detailProperty() {

    return this.detail;
  }

  /**
   * @return the subtitle property.
   */
  public StringProperty subtitleProperty() {

    return this.subtitle;
  }

  /**
   * @return the combined title and detail.
   */
  public StringExpression displayTextProperty() {

    return this.displayText;
  }

  /**
   * @return the progress property.
   */
  public DoubleProperty progressProperty() {

    return this.progress;
  }

  /**
   * @return the state property.
   */
  public ObjectProperty<TaskState> stateProperty() {

    return this.state;
  }

  /**
   * @return the stats property.
   */
  public ObjectProperty<TaskStats> statsProperty() {

    return this.stats;
  }

  /**
   * @param newStats the new value of {@link #statsProperty()}.
   */
  public void setStats(TaskStats newStats) {

    FxHelper.runFxSafe(() -> this.stats.set(newStats));
  }

  /**
   * @param newDetail the new value of {@link #detailProperty()}.
   */
  public void setDetail(String newDetail) {

    FxHelper.runFxSafe(() -> this.detail.set((newDetail == null) ? "" : newDetail));
  }

  /**
   * @param newSubtitle the new value of {@link #subtitleProperty()}.
   */
  public void setSubtitle(String newSubtitle) {

    FxHelper.runFxSafe(() -> this.subtitle.set((newSubtitle == null) ? "" : newSubtitle));
  }

  /**
   * @param newProgress the new value of {@link #progressProperty()}.
   */
  public void setProgress(double newProgress) {

    FxHelper.runFxSafe(() -> this.progress.set(newProgress));
  }

  /**
   * @param newState the new value of {@link #stateProperty()}.
   */
  public void setState(TaskState newState) {

    FxHelper.runFxSafe(() -> this.state.set(newState));
  }
}
