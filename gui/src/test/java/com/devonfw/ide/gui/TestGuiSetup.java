package com.devonfw.ide.gui;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.ide.gui.update.UpgradeController;

/**
 * Helper to centralize FXML loading and deterministic controller injection for tests.
 */
public final class TestGuiSetup {

  private TestGuiSetup() {
    // utility
  }

  /**
   * Loads the main view FXML and wires deterministic controllers.
   * <p>
   * The {@link GuiStateManager} and {@link NlsService} are provided by the caller so that the {@link MainController} and the update/upgrade controllers operate
   * on the same instance the test asserts against (there is no longer a singleton to fall back on). Null update/upgrade controllers are replaced by default
   * ones bound to the given manager.
   *
   * @param stage the {@link Stage} to display the main view on.
   * @param ideRootPath the IDE_ROOT path used by the {@link MainController} to resolve projects/workspaces.
   * @param manager the shared {@link GuiStateManager} used by all controllers and asserted on by the test.
   * @param nlsService the {@link NlsService} used by all controllers.
   * @param updateController the {@link UpdateController} to use, or {@code null} to create a default one.
   * @param upgradeController the {@link UpgradeController} to use, or {@code null} to create a default one.
   * @return the loaded main view {@link Parent}.
   * @throws IOException if the FXML cannot be loaded.
   */
  public static Parent setupStageWithControllers(Stage stage, String ideRootPath, GuiStateManager manager, NlsService nlsService,
      UpdateController updateController, UpgradeController upgradeController) throws IOException {

    URL mainViewUrl = TestGuiSetup.class.getResource("main-view.fxml");
    if (mainViewUrl == null) {
      // fallback to absolute path
      mainViewUrl = TestGuiSetup.class.getResource("/com/devonfw/ide/gui/main-view.fxml");
    }

    // If controllers are null, create default deterministic ones sharing the manager instance
    UpdateController uc = updateController == null ? new UpdateController(manager, nlsService) : updateController;
    UpgradeController ugc = upgradeController == null ? new UpgradeController(manager, nlsService) : upgradeController;
    MainController mainController = new MainController(ideRootPath, manager, uc, ugc, nlsService);

    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setResources(nlsService.getResourceBundle());
    fxmlLoader.setControllerFactory(clazz -> {
      if (clazz == ConsoleController.class) {
        return new ConsoleController(nlsService);
      } else if (clazz == MainController.class) {
        return mainController;
      }
      return null;
    });

    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus();
    stage.show();
    return root;
  }

  /**
   * Wait for a condition to become true with a timeout. Polls every 100ms.
   */
  public static void waitForCondition(java.util.function.Supplier<Boolean> condition, long timeoutMillis)
      throws InterruptedException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeoutMillis) {
      if (condition.get()) {
        return;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Condition not met within timeout");
  }
}
