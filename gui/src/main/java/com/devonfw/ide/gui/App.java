package com.devonfw.ide.gui;

import com.devonfw.tools.ide.version.IdeVersion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class App extends Application {

  /**
   * latch for waiting for the app
   */
  public static final CountDownLatch latch = new CountDownLatch(1);

  /**
   * The app itself
   */
  public static App app = null;

  /**
   * The scene to set in the window
   */
  private static Scene scene;

  /**
   * The window to show in the app
   */
  public Stage window;

  /**
   * @return the app when it is ready
   */
  public static App waitForApp() {

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return app;
  }

  @Override
  public void start(Stage stage) throws IOException {

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("hello-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 320, 240);

    stage.setTitle("IDEasy - version " + IdeVersion.get());
    stage.setScene(scene);
    stage.show();
  }
}