package com.devonfw.ide.gui;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  Parent root;

  @Override
  public void start(Stage primaryStage) throws IOException {

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
