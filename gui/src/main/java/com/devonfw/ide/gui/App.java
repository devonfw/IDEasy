package com.devonfw.ide.gui;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * GUI Application for IDEasy
 */
public class App extends Application {

  /**
   * Path to icon file used for GUI of IDEasy starting from {@code gui/src/main/resources}
   */
  public static final String ICON_PATH = "com/devonfw/ide/gui/assets/devonfw.png";

  Parent root;

  private Stage primaryStage;

  private NlsService nlsService;

  TaskManager taskManager = new TaskManager();
  GuiStateManager guiStateManager = new GuiStateManager(taskManager, null);

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

    if (SystemInfoImpl.INSTANCE.isMac()) {
      setIconInMacOsDock();
    }

    Image icon = new Image(ICON_PATH);
    primaryStage.getIcons().add(icon);
    primaryStage.setTitle("IDEasy - version " + IdeVersion.getVersionString());
    primaryStage.setScene(scene);
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
    MainController controller = new MainController(System.getenv(IdeVariables.IDE_ROOT.getName()), guiStateManager, this.nlsService);
    fxmlLoader.setControllerFactory(type -> controller);
    return fxmlLoader.load();
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
