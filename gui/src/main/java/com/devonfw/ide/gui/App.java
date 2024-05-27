package com.devonfw.ide.gui;

import com.devonfw.tools.ide.version.IdeVersion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  Parent root;

  @Override
  public void start(Stage primaryStage) throws IOException {

    root = FXMLLoader.load(App.class.getResource("main-view.fxml"));
    Scene scene = new Scene(root, 320, 240);

    primaryStage.setTitle("IDEasy - version " + IdeVersion.get());
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {

    launch(args);
  }
}
