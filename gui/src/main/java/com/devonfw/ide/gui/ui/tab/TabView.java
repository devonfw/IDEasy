package com.devonfw.ide.gui.ui.tab;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;

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
    final FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
    loader.setRoot(this);
    loader.setController(this);
    loader.setResources(nlsService.getResourceBundle());
    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
