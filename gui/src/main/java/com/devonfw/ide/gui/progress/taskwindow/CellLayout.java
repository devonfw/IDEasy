package com.devonfw.ide.gui.progress.taskwindow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import com.devonfw.ide.gui.App;

/**
 * Loads the FXML layout of a cell in the {@link TaskOverviewWindow}.
 */
final class CellLayout {

  /** Folder of the layouts belonging to the {@link TaskOverviewWindow}, relative to {@link App}. */
  private static final String LAYOUT_FOLDER = "layout/taskOverviewWindow/";

  private CellLayout() {

    // static usage only
  }

  /**
   * Loads the given layout into the given node, which acts as both the {@code fx:root} and the controller so that the {@code @FXML} fields of the node get
   * injected.
   *
   * @param view the node to load the layout into.
   * @param fxmlName the file name of the layout, relative to the task overview window layout folder.
   */
  static void load(Node view, String fxmlName) {

    URL layout = App.class.getResource(LAYOUT_FOLDER + fxmlName);
    if (layout == null) {
      throw new IllegalStateException("Cannot resolve layout " + LAYOUT_FOLDER + fxmlName);
    }
    FXMLLoader fxmlLoader = new FXMLLoader(layout);
    fxmlLoader.setRoot(view);
    fxmlLoader.setController(view);
    try {
      fxmlLoader.load();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load layout " + layout, e);
    }
  }
}
