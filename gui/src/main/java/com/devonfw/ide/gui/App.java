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

import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  Parent root;

  @Override
  public void start(Stage primaryStage) throws IOException {

    root = FXMLLoader.load(App.class.getResource("main-view.fxml"));

    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    Scene scene = new Scene(root, bounds.getWidth() / 2, bounds.getHeight() / 2);

    Image icon = new Image("com\\devonfw\\ide\\gui\\assets\\devonfw.png");
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {

    launch(args);
  }
}
