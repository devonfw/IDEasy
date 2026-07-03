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
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  Parent root;

  private static final Logger LOG = LoggerFactory.getLogger(App.class);

  @Override
  public void start(Stage primaryStage) throws IOException {

    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
          LOG.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
          Platform.runLater(() -> new IdeDialog(IdeDialog.AlertType.ERROR, throwable.getMessage()).showAndWait());
        }
    );

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
    fxmlLoader.setController(
        new MainController(System.getenv(IdeVariables.IDE_ROOT.getName()))
    );
    root = fxmlLoader.load();

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


  @SuppressWarnings("MissingJavadoc")
  public static void main(String[] args) {

    launch(args);
  }
}
