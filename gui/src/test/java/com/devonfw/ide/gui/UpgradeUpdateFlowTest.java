package com.devonfw.ide.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.ide.gui.update.UpdateService;
import com.devonfw.ide.gui.update.UpgradeController;
import com.devonfw.ide.gui.update.UpgradeService;

/**
 * Comprehensive integration tests for update and upgrade flows covering both availability and unavailability scenarios.
 * <p>
 * Tests verify: - Project update flow: when update is available and when no update is available - Tool upgrade flow: when upgrade is available and when no
 * upgrade is available - Controller interaction, UI state changes, and localized message text
 * <p>
 * Uses AppBaseTest pattern with deterministic test doubles. Nodes are looked up against the explicit {@link #root} (main view) via
 * {@link FxHelper#lookup(Parent, String)} rather than TestFX's {@code lookup}, which can resolve a stale node across the re-created scenes of the nested
 * tests. Dialog nodes are looked up against the dialog's scene root (see {@link #dialogRoot()}).
 */
public class UpgradeUpdateFlowTest extends HeadlessApplicationTest {

  @TempDir
  private static Path mockIdeRoot;
  private static final NlsService nlsService = new NlsService(null);
  private static final TaskManager taskManager = new TaskManager();
  private static GuiStateManager manager;

  /**
   * Creates the fake project structure and the shared state manager pointing at it (the FXML setup helper no longer does this). The manager must be built here,
   * after JUnit has injected the {@code @TempDir}, because the {@link MainController} resolves its projects through
   * {@link GuiStateManager#getProjectManager()}.
   */
  @BeforeAll
  public static void createFakeProjectFolderStructure() throws IOException {
    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
    manager = new GuiStateManager(taskManager, mockIdeRoot.toString());
  }

  /**
   * @return the scene root of the dialog stage opened by the update/upgrade controllers, or {@code null} if no dialog is open yet.
   */
  private Parent dialogRoot() {
    for (Window window : Window.getWindows()) {
      Stage stage = window instanceof Stage ? (Stage) window : null;
      if (stage == null || stage.getScene() == null) {
        continue;
      }
      Parent sceneRoot = stage.getScene().getRoot();
      if (sceneRoot.lookup("#executeButton") != null) {
        return sceneRoot;
      }
    }
    return null;
  }

  // ========== UPDATE FLOW TESTS: AVAILABLE ==========

  /**
   * Tests the update flow when an update is available. Updates are completed and the status transitions from available -> completed -> up to date.
   */
  @Nested
  public class UpdateAvailableTest extends HeadlessApplicationTest {

    private Parent root;

    @Override
    public void start(Stage stage) throws IOException {
      UpdateService fakeUpdateService = new UpdateService() {
        private boolean updated = false;

        @Override
        public void runUpdate(IdeGuiContext context) {
          this.updated = true;
        }

        @Override
        public boolean isUpdateAvailable(IdeGuiContext context) {
          return (context != null) && !this.updated;
        }
      };
      UpdateController testUpdateController = new UpdateController(manager, nlsService, fakeUpdateService);

      UpgradeController testUpgradeController = new UpgradeController(manager, nlsService);

      this.root = TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot.toString(), manager, nlsService, testUpdateController,
          testUpgradeController);
    }

    @Test
    public void testUpdateAvailableAndCompletes() throws InterruptedException {
      StackPane indicator = FxHelper.lookup(root, "#updateIndicator");
      ComboBox<String> selectedProject = FxHelper.<ComboBox<String>>lookup(root, "#selectedProject");
      ComboBox<String> selectedWorkspace = FxHelper.<ComboBox<String>>lookup(root, "#selectedWorkspace");

      String availableStatus = nlsService.get("status.update.available");
      String completedStatus = nlsService.get("status.update.completed");
      String upToDateStatus = nlsService.get("status.update.upToDate");

      // Select project/workspace context
      interact(() -> selectedProject.getSelectionModel().select("project-1"));
      interact(() -> selectedWorkspace.getSelectionModel().select("main"));

      // Automatic check runs and reports availability via the indicator
      TestGuiSetup.waitForCondition(indicator::isVisible, 5000);

      Tooltip tooltip = extractTooltip(indicator);
      Assertions.assertNotNull(tooltip);
      Assertions.assertEquals(nlsService.get("tooltip.update.available"), tooltip.getText());

      clickIndicator(indicator);

      // The dialog lives in its own stage; wait for it to open, then assert against its own nodes
      TestGuiSetup.waitForCondition(() -> dialogRoot() != null, 3000);
      TestGuiSetup.waitForCondition(() -> availableStatus
          .equals(FxHelper.<Label>lookup(dialogRoot(), "#statusLabel").getText()), 5000);
      Button update = FxHelper.lookup(dialogRoot(), "#executeButton");
      TestGuiSetup.waitForCondition(() -> nlsService.get("button.update").equals(update.getText()), 3000);
      TestGuiSetup.waitForCondition(() -> !update.isDisabled(), 3000);

      interact(update::fire);

      // After update, should show completed, then recheck should show up to date
      TestGuiSetup.waitForCondition(() -> completedStatus
          .equals(FxHelper.<Label>lookup(dialogRoot(), "#statusLabel").getText()), 3000);
      TestGuiSetup.waitForCondition(() -> upToDateStatus
          .equals(FxHelper.<Label>lookup(dialogRoot(), "#statusLabel").getText()), 3000);
    }

  }

  // ========== UPDATE FLOW TESTS: UNAVAILABLE ==========

  /**
   * Tests the update flow when no update is available. The status should show "Up to date" when a project context is selected.
   */
  @Nested
  public class UpdateUnavailableTest extends HeadlessApplicationTest {

    private Parent root;

    @Override
    public void start(Stage stage) throws IOException {
      UpdateService fakeUpdateService = new UpdateService() {
        @Override
        public boolean isUpdateAvailable(IdeGuiContext context) {
          return false;
        }
      };
      UpdateController testUpdateController = new UpdateController(manager, nlsService, fakeUpdateService);

      UpgradeController testUpgradeController = new UpgradeController(manager, nlsService);

      this.root = TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot.toString(), manager, nlsService, testUpdateController,
          testUpgradeController);
    }

    @Test
    public void testUpdateShowsUpToDateWhenNoUpdateAvailable() throws InterruptedException {
      StackPane indicator = FxHelper.lookup(root, "#updateIndicator");
      ComboBox<String> selectedProject = FxHelper.<ComboBox<String>>lookup(root, "#selectedProject");
      ComboBox<String> selectedWorkspace = FxHelper.<ComboBox<String>>lookup(root, "#selectedWorkspace");

      interact(() -> selectedProject.getSelectionModel().select("project-1"));
      interact(() -> selectedWorkspace.getSelectionModel().select("main"));

      TestGuiSetup.waitForCondition(() -> !indicator.isVisible(), 3000);
    }

  }

  // ========== UPGRADE FLOW TESTS: AVAILABLE ==========

  /**
   * Tests the upgrade flow when an upgrade is available. Extends AppBaseTest which provides a deterministic UpgradeController that reports upgrade available
   * until upgrade is performed, then reports up-to-date.
   * <p>
   * Tests the cycle: indicator visible -> click -> dialog opens -> button enabled -> click -> indicator hidden
   */
  @Nested
  public class UpgradeAvailableTest extends HeadlessApplicationTest {

    private Parent root;

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(manager, nlsService);

      UpgradeService fakeUpgradeService = new UpgradeService(manager) {
        private boolean upgraded = false;

        @Override
        public void runUpgrade() {
          this.upgraded = true;
        }

        @Override
        public boolean checkForUpgrade() {
          return !this.upgraded;
        }
      };
      UpgradeController testUpgradeController = new UpgradeController(nlsService, fakeUpgradeService);

      this.root = TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot.toString(), manager, nlsService, testUpdateController,
          testUpgradeController);
    }

    @Test
    public void testUpgradeAvailableAndCompletes() throws InterruptedException {
      StackPane indicator = FxHelper.lookup(root, "#upgradeIndicator");

      String availableStatus = MessageFormat.format(nlsService.get("status.upgrade.available"), "", "");

      // 1. Wait for indicator to become visible (indicates upgrade available)
      TestGuiSetup.waitForCondition(indicator::isVisible, 5000);

      // 2. Click the indicator to show the upgrade dialog
      clickIndicator(indicator);

      // 3. Wait for dialog to open and get the upgrade button
      TestGuiSetup.waitForCondition(() -> dialogRoot() != null, 3000);
      Button executeButton = FxHelper.lookup(dialogRoot(), "#executeButton");
      Label upgradeStatus = FxHelper.lookup(dialogRoot(), "#statusLabel");

      // 4. Verify button is enabled (indicating upgrade is available)
      String upgradeStatusText = upgradeStatus.getText();
      TestGuiSetup.waitForCondition(() -> availableStatus.equals(upgradeStatusText), 3000);
      TestGuiSetup.waitForCondition(() -> !executeButton.isDisabled(), 3000);

      // 5. Click the upgrade button
      interact(executeButton::fire);

      // 6. After upgrade completes, indicator should be hidden (no more upgrade available)
      TestGuiSetup.waitForCondition(() -> !indicator.isVisible(), 5000);
    }

  }

  // ========== UPGRADE FLOW TESTS: UNAVAILABLE ==========

  /**
   * Tests the upgrade flow when no upgrade is available. The upgrade indicator should not be visible and the status should show "is the latest version".
   */
  @Nested
  public class UpgradeUnavailableTest extends HeadlessApplicationTest {

    private Parent root;

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(manager, nlsService);

      UpgradeService fakeUpgradeService = new UpgradeService(manager) {
        @Override
        public boolean checkForUpgrade() {
          return false;
        }
      };
      UpgradeController testUpgradeController = new UpgradeController(nlsService, fakeUpgradeService);

      this.root = TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot.toString(), manager, nlsService, testUpdateController,
          testUpgradeController);
    }

    @Test
    public void testUpgradeIndicatorHiddenWhenNoUpgradeAvailable() throws InterruptedException {
      StackPane indicator = FxHelper.lookup(root, "#upgradeIndicator");

      // Wait for check to complete then verify indicator is not visible
      TestGuiSetup.waitForCondition(() -> !indicator.isVisible(), 5000);
    }
  }

  private Tooltip extractTooltip(StackPane node) {
    return node.getProperties().values().stream().filter(Tooltip.class::isInstance).map(Tooltip.class::cast).findFirst().orElse(null);
  }

  /**
   * Fires a {@link MouseEvent#MOUSE_CLICKED} directly on the given indicator node.
   * <p>
   * TestFX's {@code clickOn} is coordinate-based and does not reliably deliver the click to the small, translated indicator in the headless (Monocle)
   * environment, so the controller's {@code setOnMouseClicked} handler never fires. Firing the event on the node directly is the reliable alternative, as
   * used elsewhere in the GUI test suite (see {@code AppBaseTest}).
   */
  private void clickIndicator(StackPane indicator) {
    interact(() -> indicator.fireEvent(
        new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, null, 1, false, false, false, false, false, false, false, false, false, false, null)));
  }
}
