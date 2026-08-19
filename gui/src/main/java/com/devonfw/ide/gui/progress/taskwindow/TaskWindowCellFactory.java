package com.devonfw.ide.gui.progress.taskwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.progress.GuiTask;
import com.devonfw.ide.gui.progress.TaskState;
import com.devonfw.ide.gui.progress.TaskStats;

/**
 * Cell factory for displaying the tasks in the {@link TaskOverviewWindow}.
 * <p>
 * A cell renders one of two layouts, both declared as FXML. A top-level task gets {@link TaskCellView}, shared by progress bars and steps so that they look
 * and behave alike; a sub-step of an expanded task gets the compact {@link SubStepCellView}. This class only binds those nodes to the task - the layout itself
 * lives in the FXML.
 */
public class TaskWindowCellFactory implements Callback<TreeView<GuiTask>, TreeCell<GuiTask>> {

  /** Colour of a successful outcome. */
  static final String SUCCESS_COLOR = "#1e7e34";

  /** Colour of a failed outcome. */
  static final String FAILURE_COLOR = "#c5221f";

  private final TaskManager taskManager;

  /**
   * @param taskManager the {@link TaskManager} used to dismiss finished tasks.
   */
  public TaskWindowCellFactory(TaskManager taskManager) {

    this.taskManager = taskManager;
  }

  /**
   * @param state the {@link TaskState}.
   * @return the symbol to display for the given {@code state}.
   */
  static String stateSymbol(TaskState state) {

    return switch (state) {
      case SUCCESS -> "✓";
      case FAILED -> "✗";
      case RUNNING -> "";
    };
  }

  @Override
  public TreeCell<GuiTask> call(TreeView<GuiTask> param) {

    return new TreeCell<>() {

      private final TaskCellView taskView = new TaskCellView();

      private final SubStepCellView subStepView = new SubStepCellView();

      @Override
      public void updateItem(GuiTask task, boolean empty) {

        super.updateItem(task, empty);

        // Cells get recycled, so previous bindings must always be released first. This has to happen synchronously - deferring it to
        // Platform.runLater() would bind a recycled cell to a task it no longer displays.
        unbindAll();

        if (empty || (task == null)) {
          setText(null);
          setGraphic(null);
          return;
        }

        if (isSubStep()) {
          bindSubStepRow(task);
          setGraphic(this.subStepView);
        } else {
          bindTaskRow(task);
          setGraphic(this.taskView);
        }
      }

      /**
       * @return {@code true} if this cell shows a sub-step rather than a top-level task. The hidden root carries a {@code null} value, so a task sits directly
       *     below it while a sub-step sits below a task.
       */
      private boolean isSubStep() {

        TreeItem<GuiTask> item = getTreeItem();
        TreeItem<GuiTask> parent = (item == null) ? null : item.getParent();
        return (parent != null) && (parent.getValue() != null);
      }

      private void bindTaskRow(GuiTask task) {

        this.taskView.getTitleLabel().textProperty().bind(task.displayTextProperty());
        this.taskView.getSubtitleLabel().textProperty().bind(task.subtitleProperty());
        this.taskView.getSubtitleLabel().visibleProperty().bind(task.subtitleProperty().isNotEmpty());
        this.taskView.getProgressBar().progressProperty().bind(task.progressProperty());
        this.taskView.getProgressBar().visibleProperty().bind(task.stateProperty().isEqualTo(TaskState.RUNNING));
        this.taskView.getStateLabel().textProperty()
            .bind(Bindings.createStringBinding(() -> stateSymbol(task.getState()), task.stateProperty()));
        bindChips(task);
        if (task.isDismissable()) {
          this.taskView.getDismissButton().visibleProperty().bind(task.stateProperty().isNotEqualTo(TaskState.RUNNING));
          this.taskView.getDismissButton().setOnAction(_ -> TaskWindowCellFactory.this.taskManager.removeTask(task));
        } else {
          this.taskView.getDismissButton().setVisible(false);
        }
      }

      /**
       * A sub-step shows a spinner while it runs and a coloured mark once it has ended, in the same fixed-size box either way.
       */
      private void bindSubStepRow(GuiTask task) {

        ReadOnlyObjectProperty<TaskState> state = task.stateProperty();
        this.subStepView.getTitleLabel().textProperty().bind(task.displayTextProperty());
        this.subStepView.getSpinner().visibleProperty().bind(state.isEqualTo(TaskState.RUNNING));
        this.subStepView.getMark().visibleProperty().bind(state.isNotEqualTo(TaskState.RUNNING));
        this.subStepView.getMark().textProperty().bind(Bindings.createStringBinding(() -> stateSymbol(task.getState()), state));
        this.subStepView.getMark().styleProperty().bind(Bindings.createStringBinding(
            () -> "-fx-text-fill: " + ((task.getState() == TaskState.FAILED) ? FAILURE_COLOR : SUCCESS_COLOR) + ";", state));
      }

      /**
       * Binds the sub-step tally to a chip per outcome. A chip only appears once its count is non-zero, so a task without sub-steps shows none at all.
       */
      private void bindChips(GuiTask task) {

        ReadOnlyObjectProperty<TaskStats> stats = task.statsProperty();
        this.taskView.getSucceededChip().textProperty().bind(Bindings.createStringBinding(() -> "✓ " + stats.get().succeeded(), stats));
        this.taskView.getSucceededChip().visibleProperty().bind(Bindings.createBooleanBinding(() -> stats.get().succeeded() > 0, stats));
        this.taskView.getFailedChip().textProperty().bind(Bindings.createStringBinding(() -> "✗ " + stats.get().failed(), stats));
        this.taskView.getFailedChip().visibleProperty().bind(Bindings.createBooleanBinding(() -> stats.get().failed() > 0, stats));
        this.taskView.getChipBox().visibleProperty()
            .bind(Bindings.createBooleanBinding(() -> (stats.get().succeeded() > 0) || (stats.get().failed() > 0), stats));
      }

      private void unbindAll() {

        this.taskView.getTitleLabel().textProperty().unbind();
        this.taskView.getSubtitleLabel().textProperty().unbind();
        this.taskView.getSubtitleLabel().visibleProperty().unbind();
        this.taskView.getProgressBar().progressProperty().unbind();
        this.taskView.getProgressBar().visibleProperty().unbind();
        this.taskView.getStateLabel().textProperty().unbind();
        this.taskView.getDismissButton().visibleProperty().unbind();
        this.taskView.getDismissButton().setOnAction(null);
        this.taskView.getSucceededChip().textProperty().unbind();
        this.taskView.getSucceededChip().visibleProperty().unbind();
        this.taskView.getFailedChip().textProperty().unbind();
        this.taskView.getFailedChip().visibleProperty().unbind();
        this.taskView.getChipBox().visibleProperty().unbind();
        this.subStepView.getTitleLabel().textProperty().unbind();
        this.subStepView.getSpinner().visibleProperty().unbind();
        this.subStepView.getMark().visibleProperty().unbind();
        this.subStepView.getMark().textProperty().unbind();
        this.subStepView.getMark().styleProperty().unbind();
      }
    };
  }
}
