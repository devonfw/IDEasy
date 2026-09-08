package com.devonfw.ide.gui;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.mainwindow.MainWindow;
import com.devonfw.ide.gui.ui.modal.IdeDialog;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  /**
   * Path to icon file used for GUI of IDEasy starting from {@code gui/src/main/resources}
   */
  public static final String ICON_PATH = "com/devonfw/ide/gui/assets/devonfw.png";

  private Stage primaryStage;

  private NlsService nlsService;

  TaskManager taskManager = new TaskManager();
  GuiStateManager guiStateManager = new GuiStateManager(taskManager, null);

  private final Logger LOG = LoggerFactory.getLogger(App.class);

  @Override
  public void start(Stage primaryStage) {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      LOG.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
      Platform.runLater(() -> new IdeDialog(IdeDialog.AlertType.ERROR, throwable.getMessage()).showAndWait());
    });

    this.primaryStage = primaryStage;

    this.nlsService = new NlsService(null);

    final MainWindow mainWindow = new MainWindow(guiStateManager, guiStateManager.getProjectManager(), this.nlsService);

    //this.nlsService.addLocaleChangeListener(this::reloadMainView);

    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    Scene scene = new Scene(mainWindow, bounds.getWidth() / 2, bounds.getHeight() / 2);

    if (SystemInfoImpl.INSTANCE.isMac()) {
      setIconInMacOsDock();
    }

    Image icon = new Image(ICON_PATH);
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(scene);
    primaryStage.setWidth(scene.getWidth());
    primaryStage.setHeight(scene.getHeight());
    primaryStage.setMinWidth(scene.getWidth());
    primaryStage.setMinHeight(scene.getHeight());
    primaryStage.show();

    primaryStage.setOnCloseRequest(event -> {

      LOG.info("Closing application");
      if (!taskManager.getTasks().isEmpty()) {
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

  @Override
  public void stop() {

    //this.nlsService.removeLocaleChangeListener(this::reloadMainView);
  }

  private void setIconInMacOsDock() {
    try {
      Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
      URL imageResource = getClass().getClassLoader().getResource(ICON_PATH);
      java.awt.Image image = defaultToolkit.getImage(imageResource);

      Taskbar taskbar = Taskbar.getTaskbar();
      taskbar.setIconImage(image);
    } catch (UnsupportedOperationException e) {
      LOG.error("Failed to set IDEasy icon in MacOS dock. ", e);
    }
  }

  @SuppressWarnings("MissingJavadoc")
  public static void main(String[] args) {

    launch(args);
  }
}
