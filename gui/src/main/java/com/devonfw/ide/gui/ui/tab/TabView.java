package com.devonfw.ide.gui.ui.tab;

import javafx.scene.control.ScrollPane;

import com.devonfw.ide.gui.helper.FxHelper;
import com.devonfw.ide.gui.service.NlsService;

/**
 * Base class for the views of all tabs.
 */
public abstract class TabView extends ScrollPane {

  /**
   * Loads this view's FXML file.
   *
   * @param fxmlName the FXML file name co-located with the concrete tab view (e.g. {@code "IdeLauncher.fxml"}).
   * @param nlsService the localization service; its resource bundle resolves the {@code %key} text keys in the FXML.
   */
  protected void loadFxml(String fxmlName, NlsService nlsService) {
    FxHelper.loadFxml(fxmlName, nlsService, this);
  }

}
