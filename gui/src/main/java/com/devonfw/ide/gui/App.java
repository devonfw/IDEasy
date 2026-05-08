package com.devonfw.ide.gui;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.progress.TaskManager;
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
          System.out.println("Uncaught exception in thread " + thread.getName() + ":" + throwable.getMessage());
          throwable.printStackTrace();
          Platform.runLater(() -> new IdeDialog(IdeDialog.AlertType.ERROR, throwable.getMessage()).showAndWait());
        }
    );

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
    fxmlLoader.setController(
        new MainController(System.getenv(IdeVariables.IDE_ROOT.getName()))
    );
    root = fxmlLoader.load();

    Scene scene = new Scene(root, ((BorderPane) fxmlLoader.getRoot()).getPrefWidth(), ((BorderPane) fxmlLoader.getRoot()).getPrefHeight());
    Image icon = new Image("com/devonfw/ide/gui/assets/devonfw.png");
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(scene);
    primaryStage.setMinWidth(scene.getWidth());
    primaryStage.setMinHeight(scene.getHeight());
    primaryStage.show();

    primaryStage.setOnCloseRequest(event -> {
      LOG.info("Closing application");
      if (!TaskManager.getInstance().getTasks().isEmpty()) {
        IdeDialog closeConfirm = new IdeDialog(IdeDialog.AlertType.CONFIRMATION, "There are still running tasks. Are you sure you want to exit?",
            ButtonType.CLOSE, ButtonType.CANCEL);
        closeConfirm.showAndWait().ifPresent(response -> {
          if (response == ButtonType.CLOSE) {
            exitApplication();
          } else {
            event.consume();
          }
        });
      } else {
        exitApplication();
      }
    });
  }

  private void exitApplication() {
    Platform.exit();
    System.exit(0);
  }


  @SuppressWarnings("MissingJavadoc")
  public static void main(String[] args) {

    launch(args);
  }
}
