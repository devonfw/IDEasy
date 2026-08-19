package com.devonfw.ide.gui.progress.taskwindow;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

/**
 * The row of a top-level task in the {@link TaskOverviewWindow}, loaded from {@code task_cell.fxml}.
 * <p>
 * This class only exposes the nodes; what they show is bound to the task by {@link TaskWindowCellFactory}.
 */
public class TaskCellView extends HBox {

  @FXML
  private Label stateLabel;

  @FXML
  private Label titleLabel;

  @FXML
  private Label subtitleLabel;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private Label succeededChip;

  @FXML
  private Label failedChip;

  @FXML
  private HBox chipBox;

  @FXML
  private Button dismissButton;

  /**
   * The constructor.
   */
  public TaskCellView() {

    CellLayout.load(this, "task_cell.fxml");
  }

  /**
   * @return the label showing the ✓ or ✗ of a finished task.
   */
  public Label getStateLabel() {

    return this.stateLabel;
  }

  /**
   * @return the label showing the title and detail of the task.
   */
  public Label getTitleLabel() {

    return this.titleLabel;
  }

  /**
   * @return the label naming the sub-step that is currently running.
   */
  public Label getSubtitleLabel() {

    return this.subtitleLabel;
  }

  /**
   * @return the progress bar of the task.
   */
  public ProgressBar getProgressBar() {

    return this.progressBar;
  }

  /**
   * @return the chip counting the successful sub-steps.
   */
  public Label getSucceededChip() {

    return this.succeededChip;
  }

  /**
   * @return the chip counting the failed sub-steps.
   */
  public Label getFailedChip() {

    return this.failedChip;
  }

  /**
   * @return the container of both chips.
   */
  public HBox getChipBox() {

    return this.chipBox;
  }

  /**
   * @return the button removing a finished task from the list.
   */
  public Button getDismissButton() {

    return this.dismissButton;
  }
}
