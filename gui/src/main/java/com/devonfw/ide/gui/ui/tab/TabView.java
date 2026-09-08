package com.devonfw.ide.gui.ui.tab;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;

import com.devonfw.ide.gui.service.NlsService;

/**
 * Base class for the views of all logic-bearing tabs. A tab is a scrollable content area, so every tab view is a {@link ScrollPane}; concrete tab views extend
 * this and pair with a {@link TabViewModel}. The {@link #loadFxml(String, NlsService)} method removes the otherwise-identical {@link FXMLLoader} setup from
 * every view constructor.
 */
public abstract class TabView extends ScrollPane {

  /**
   * Loads this view's FXML file, wiring {@code this} as both root and controller and applying the localization bundle so the {@code %key} text resources
   * resolve.
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
