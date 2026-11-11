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

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.mainwindow.MainWindowView;
import com.devonfw.ide.gui.core.mainwindow.MainWindowViewModel;
import com.devonfw.ide.gui.core.modal.IdeDialog;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  /**
   * Path to icon file used for GUI of IDEasy starting from {@code gui/src/main/resources}
   */
  public static final String ICON_PATH = "com/devonfw/ide/gui/assets/icon.png";

  /** Preferred initial width of the main window content in pixels, as given by the UI design (excluding window decorations). */
  private static final double MAIN_WINDOW_WIDTH = 1295.0;

  /** Preferred initial height of the main window content in pixels, as given by the UI design (excluding window decorations). */
  private static final double MAIN_WINDOW_HEIGHT = 691.0;

  /** Minimum width of the main window content in pixels (excluding window decorations). */
  private static final double MAIN_WINDOW_MIN_WIDTH = 900.0;

  /** Minimum height of the main window content in pixels (excluding window decorations). */
  private static final double MAIN_WINDOW_MIN_HEIGHT = 600.0;

  private Stage primaryStage;

  GuiStateManager guiStateManager = new GuiStateManager(null);

  private final Logger LOG = LoggerFactory.getLogger(App.class);

  @Override
  public void start(Stage primaryStage) {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      LOG.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
      Platform.runLater(() -> new IdeDialog(IdeDialog.AlertType.ERROR, throwable.getMessage()).showAndWait());
    });

    this.primaryStage = primaryStage;

    final MainWindowViewModel mainWindowViewModel = new MainWindowViewModel(guiStateManager);
    final MainWindowView mainWindow = new MainWindowView(mainWindowViewModel, guiStateManager);

    //this.nlsService.addLocaleChangeListener(this::reloadMainView);

    configureMainWindow(new Scene(mainWindow, MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT));

    primaryStage.setOnCloseRequest(event -> {

      LOG.info("Closing application");
      if (!guiStateManager.getTaskManager().getTasks().isEmpty()) {
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

  private void configureMainWindow(Scene mainWindowScene) {
    if (SystemInfoImpl.INSTANCE.isMac()) {
      setIconInMacOsDock();
    }

    Image icon = new Image(ICON_PATH);
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(mainWindowScene);
    primaryStage.show();
    // the size of the window decorations (title bar, borders) is only known once the stage is shown
    double decorationWidth = primaryStage.getWidth() - mainWindowScene.getWidth();
    double decorationHeight = primaryStage.getHeight() - mainWindowScene.getHeight();
    // the stage including its decorations must still fit on the screen (excluding e.g. the taskbar)
    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    primaryStage.setMinWidth(Math.min(MAIN_WINDOW_MIN_WIDTH + decorationWidth, bounds.getWidth()));
    primaryStage.setMinHeight(Math.min(MAIN_WINDOW_MIN_HEIGHT + decorationHeight, bounds.getHeight()));
    primaryStage.setWidth(Math.min(MAIN_WINDOW_WIDTH + decorationWidth, bounds.getWidth()));
    primaryStage.setHeight(Math.min(MAIN_WINDOW_HEIGHT + decorationHeight, bounds.getHeight()));
    primaryStage.centerOnScreen();
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
