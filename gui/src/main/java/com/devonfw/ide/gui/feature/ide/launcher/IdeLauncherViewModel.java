package com.devonfw.ide.gui.feature.ide.launcher;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.tab.TabViewModel;

/**
 * View model of the IDE launcher tab. Launch buttons are disabled until a workspace is selected and while their IDE is starting.
 */
public class IdeLauncherViewModel extends TabViewModel {

  private final CommandletService commandletService;

  private final BooleanProperty isIdeButtonsDisabledProperty = new SimpleBooleanProperty();

  /**
   * Creates the view model.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public IdeLauncherViewModel(GuiStateManager guiStateManager) {
    super(guiStateManager);
    this.commandletService = guiStateManager.getCommandletService();

    this.isIdeButtonsDisabledProperty.bind(this.guiStateManager.workspaceSelectedProperty().not());
  }

  @Override
  public String getTabTitleKey() {
    return "tab.ide_launcher";
  }

  /**
   * Runs the given IDE commandlet in the currently selected project/workspace context.
   *
   * @param commandlet the commandlet (IDE) to start.
   */
  public void runCommandlet(String commandlet) {
    this.commandletService.runCommandlet(commandlet);
  }

  public BooleanProperty isIdeButtonsDisabledProperty() {
    return isIdeButtonsDisabledProperty;
  }

  /**
   * @param commandlet the IDE controlled by the button.
   * @return whether no workspace is selected or this IDE is already starting.
   */
  public BooleanBinding ideButtonDisabledBinding(String commandlet) {

    return this.isIdeButtonsDisabledProperty.or(this.commandletService.runningBinding(commandlet));
  }

}
