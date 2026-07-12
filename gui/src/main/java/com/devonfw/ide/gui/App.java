package com.devonfw.ide.gui;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  Parent root;

  private Stage primaryStage;

  private NlsService nlsService;

  private static final Logger LOG = LoggerFactory.getLogger(App.class);

  @Override
  public void start(Stage primaryStage) throws IOException {

    this.primaryStage = primaryStage;

    this.nlsService = new NlsService(null);

    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
          LOG.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
          Platform.runLater(() -> new IdeDialog(IdeDialog.AlertType.ERROR, throwable.getMessage()).showAndWait());
        }
    );

    root = loadMainView();

    this.nlsService.addLocaleChangeListener(this::reloadMainView);

    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    Scene scene = new Scene(root, bounds.getWidth() / 2, bounds.getHeight() / 2);

    Image icon = new Image("com/devonfw/ide/gui/assets/devonfw.png");
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(scene);
    primaryStage.setMinWidth(scene.getWidth());
    primaryStage.setMinHeight(scene.getHeight());
    primaryStage.show();
  }

  @Override
  public void stop() {

    this.nlsService.removeLocaleChangeListener(this::reloadMainView);
  }

  private void reloadMainView() {

    try {
      Parent reloadedRoot = loadMainView();
      this.root = reloadedRoot;
      if (this.primaryStage != null && this.primaryStage.getScene() != null) {
        this.primaryStage.getScene().setRoot(reloadedRoot);
      }
    } catch (IOException e) {
      LOG.error("Failed to reload main view after locale change", e);
    }
  }

  private Parent loadMainView() throws IOException {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
    fxmlLoader.setResources(this.nlsService.getResourceBundle());
    fxmlLoader.setController(new MainController(System.getenv(IdeVariables.IDE_ROOT.getName()), this.nlsService));
    return fxmlLoader.load();
  }


  @SuppressWarnings("MissingJavadoc")
  public static void main(String[] args) {

    launch(args);
  }
}
