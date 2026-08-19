package com.devonfw.ide.gui.progress.taskwindow;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

/**
 * The row of a single sub-step below an expanded task, loaded from {@code sub_step_cell.fxml}.
 * <p>
 * This class only exposes the nodes; what they show is bound to the sub-step by {@link TaskWindowCellFactory}.
 */
public class SubStepCellView extends HBox {

  @FXML
  private ProgressIndicator spinner;

  @FXML
  private Label mark;

  @FXML
  private Label titleLabel;

  /**
   * The constructor.
   */
  public SubStepCellView() {

    CellLayout.load(this, "sub_step_cell.fxml");
  }

  /**
   * @return the spinner shown while the sub-step is running.
   */
  public ProgressIndicator getSpinner() {

    return this.spinner;
  }

  /**
   * @return the label showing the ✓ or ✗ once the sub-step has ended, drawn in the same spot as the {@link #getSpinner() spinner}.
   */
  public Label getMark() {

    return this.mark;
  }

  /**
   * @return the label showing the name of the sub-step.
   */
  public Label getTitleLabel() {

    return this.titleLabel;
  }
}
