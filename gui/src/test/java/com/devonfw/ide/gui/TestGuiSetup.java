package com.devonfw.ide.gui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.i18n.I18nService;
import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.ide.gui.update.UpgradeController;

/**
 * Helper to centralize FXML loading and deterministic controller injection for tests.
 */
public final class TestGuiSetup {

  private TestGuiSetup() {
    // utility
  }

  public static Parent setupStageWithControllers(Stage stage, Path mockIdeRoot, UpdateController updateController,
      UpgradeController upgradeController) throws IOException {

    // Initialize i18n
    I18nService.resetInstance();
    I18nService.getInstance(Locale.ENGLISH);

    // Ensure fake project structure exists when provided
    if (mockIdeRoot != null) {
      try {
        FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
      } catch (IOException e) {
        // ignore, caller may handle
      }
      IdeGuiStateManager.getInstanceOverrideRootDir(mockIdeRoot.toString()).clearCurrentContext();
    }

    URL mainViewUrl = TestGuiSetup.class.getResource("main-view.fxml");
    if (mainViewUrl == null) {
      // fallback to absolute path
      mainViewUrl = TestGuiSetup.class.getResource("/com/devonfw/ide/gui/main-view.fxml");
    }

    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setResources(I18nService.getInstance().getResourceBundle());

    // If controllers are null, create default deterministic ones
    UpdateController uc = updateController == null ? new UpdateController(IdeGuiStateManager.getInstance()) : updateController;
    UpgradeController ugc = upgradeController == null ? new UpgradeController(IdeGuiStateManager.getInstance()) : upgradeController;

    fxmlLoader.setController(new MainController(mockIdeRoot != null ? mockIdeRoot.toString() : System.getProperty("java.io.tmpdir"),
        IdeGuiStateManager.getInstance().getProjectManager(), uc, ugc));

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

