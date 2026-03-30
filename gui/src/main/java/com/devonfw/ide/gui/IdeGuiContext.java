package com.devonfw.ide.gui;

import java.nio.file.Path;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarNone;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashbaord (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {


  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeGuiContext(IdeStartContextImpl startContext, Path workingDirectory) {
    super(startContext, workingDirectory);
  }

  @Override
  protected String readLine() {

    return "";
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {

    return new IdeProgressBarNone(title, 0, unitName, unitSize);
  }

  @Override
  public boolean question(String question, Object... args) {

    IdeDialog dialog = new IdeDialog(IdeDialog.AlertType.CONFIRMATION, question);
    dialog.showAndWait();
    return dialog.getResult() == ButtonType.YES;
  }

  @Override
  public <O> O question(O[] options, String question, Object... args) {

    IdeDialog dialog = new IdeDialog(Alert.AlertType.CONFIRMATION, question, ButtonType.APPLY, ButtonType.CANCEL);

    dialog.getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);

    DialogPane pane = dialog.getDialogPane();
    ToggleGroup group = new ToggleGroup();

    VBox vbox = new VBox(10);
    for (O option : options) {
      RadioButton button = new RadioButton(option.toString());
      button.setOnAction(e -> dialog.getDialogPane().lookupButton(ButtonType.APPLY).setDisable(false));
      button.setToggleGroup(group);
      vbox.getChildren().add(button);
    }
    pane.setContent(vbox);
    dialog.showAndWait();

    RadioButton selectedOption = (RadioButton) group.getSelectedToggle();

    return selectedOption == null ? null : (O) selectedOption.getText();
  }
}
