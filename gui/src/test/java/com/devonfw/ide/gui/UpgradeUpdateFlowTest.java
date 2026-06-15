package com.devonfw.ide.gui;

import java.io.IOException;
import java.nio.file.Path;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.i18n.I18nService;
import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.ide.gui.update.UpgradeController;

/**
 * Comprehensive integration tests for update and upgrade flows covering both availability and unavailability scenarios.
 * <p>
 * Tests verify: - Project update flow: when update is available and when no update is available - Tool upgrade flow: when upgrade is available and when no
 * upgrade is available - Controller interaction, UI state changes, and localized message text
 * <p>
 * Uses AppBaseTest pattern with deterministic test doubles.
 */
public class UpgradeUpdateFlowTest extends HeadlessApplicationTest {

  @TempDir
  private static Path mockIdeRoot;

  // ========== UPDATE FLOW TESTS: AVAILABLE ==========

  /**
   * Tests the update flow when an update is available. Updates are completed and the status transitions from available -> completed -> up to date.
   */
  @Nested
  public class UpdateAvailableTest extends HeadlessApplicationTest {

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(IdeGuiStateManager.getInstance()) {
        private boolean updated = false;

        @Override
        protected void performProjectUpdate(IdeGuiContext context) {
          this.updated = true;
        }

        @Override
        protected boolean checkForUpdates(IdeGuiContext context) {
          return (context != null) && !this.updated;
        }
      };

      UpgradeController testUpgradeController = new UpgradeController(IdeGuiStateManager.getInstance()) {
        private boolean upgraded = false;

        @Override
        protected void performUpgrade() {
          this.upgraded = true;
        }

        @Override
        protected boolean checkForUpgrade() {
          return !this.upgraded;
        }
      };

      TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot, testUpdateController, testUpgradeController);
    }

    @Test
    public void testUpdateAvailableAndCompletes() throws InterruptedException {
      Label status = lookup("#updateStatusLabel").queryAs(Label.class);
      Button update = lookup("#updateButton").queryAs(Button.class);
      @SuppressWarnings("unchecked")
      ComboBox<String> selectedProject = (ComboBox<String>) lookup("#selectedProject").queryAs(ComboBox.class);
      @SuppressWarnings("unchecked")
      ComboBox<String> selectedWorkspace = (ComboBox<String>) lookup("#selectedWorkspace").queryAs(ComboBox.class);

      I18nService i18n = I18nService.getInstance();
      String availableStatus = i18n.get("status.update.available");
      String completedStatus = i18n.get("status.update.completed");
      String upToDateStatus = i18n.get("status.update.upToDate");

      // Select project/workspace context
      interact(() -> selectedProject.getSelectionModel().select("project-1"));
      interact(() -> selectedWorkspace.getSelectionModel().select("main"));

      // Automatic check runs and reports availability
      TestGuiSetup.waitForCondition(() -> availableStatus.equals(status.getText()), 5000);

      interact(update::fire);

      // After update, should show completed, then recheck should show up to date
      TestGuiSetup.waitForCondition(() -> completedStatus.equals(status.getText()), 3000);
      TestGuiSetup.waitForCondition(() -> upToDateStatus.equals(status.getText()), 3000);
    }

  }

  // ========== UPDATE FLOW TESTS: UNAVAILABLE ==========

  /**
   * Tests the update flow when no update is available. The status should show "Up to date" when a project context is selected.
   */
  @Nested
  public class UpdateUnavailableTest extends HeadlessApplicationTest {

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(IdeGuiStateManager.getInstance()) {
        @Override
        protected boolean checkForUpdates(IdeGuiContext context) {
          return false;
        }
      };

      UpgradeController testUpgradeController = new UpgradeController(IdeGuiStateManager.getInstance());

      TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot, testUpdateController, testUpgradeController);
    }

    @Test
    public void testUpdateShowsUpToDateWhenNoUpdateAvailable() throws InterruptedException {
      Label status = lookup("#updateStatusLabel").queryAs(Label.class);
      @SuppressWarnings("unchecked")
      ComboBox<String> selectedProject = (ComboBox<String>) lookup("#selectedProject").queryAs(ComboBox.class);
      @SuppressWarnings("unchecked")
      ComboBox<String> selectedWorkspace = (ComboBox<String>) lookup("#selectedWorkspace").queryAs(ComboBox.class);

      String expected = I18nService.getInstance().get("status.update.upToDate");

      interact(() -> selectedProject.getSelectionModel().select("project-1"));
      interact(() -> selectedWorkspace.getSelectionModel().select("main"));

      TestGuiSetup.waitForCondition(() -> expected.equals(status.getText()), 3000);
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

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(IdeGuiStateManager.getInstance()) {
        private boolean updated = false;

        @Override
        protected void performProjectUpdate(IdeGuiContext context) {
          this.updated = true;
        }

        @Override
        protected boolean checkForUpdates(IdeGuiContext context) {
          return (context != null) && !this.updated;
        }
      };

      UpgradeController testUpgradeController = new UpgradeController(IdeGuiStateManager.getInstance()) {
        private boolean upgraded = false;

        @Override
        protected void performUpgrade() {
          this.upgraded = true;
        }

        @Override
        protected boolean checkForUpgrade() {
          return !this.upgraded;
        }
      };

      TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot, testUpdateController, testUpgradeController);
    }

    @Test
    public void testUpgradeAvailableAndCompletes() throws InterruptedException {
      Circle indicator = lookup("#upgradeIndicator").queryAs(Circle.class);

      // 1. Wait for indicator to become visible (indicates upgrade available)
      TestGuiSetup.waitForCondition(indicator::isVisible, 5000);

      // 2. Click the indicator to show the upgrade dialog
      clickOn(indicator);

      // 3. Wait for dialog to open and get the upgrade button
      TestGuiSetup.waitForCondition(() -> lookup("#upgradeButton").tryQuery().isPresent(), 3000);
      Button upgradeButton = lookup("#upgradeButton").queryAs(Button.class);

      // 4. Verify button is enabled (indicating upgrade is available)
      TestGuiSetup.waitForCondition(() -> !upgradeButton.isDisabled(), 3000);

      // 5. Click the upgrade button
      interact(upgradeButton::fire);

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

    @Override
    public void start(Stage stage) throws IOException {
      UpdateController testUpdateController = new UpdateController(IdeGuiStateManager.getInstance());

      UpgradeController testUpgradeController = new UpgradeController(IdeGuiStateManager.getInstance()) {
        @Override
        protected boolean checkForUpgrade() {
          return false;
        }
      };

      TestGuiSetup.setupStageWithControllers(stage, mockIdeRoot, testUpdateController, testUpgradeController);
    }

    @Test
    public void testUpgradeIndicatorHiddenWhenNoUpgradeAvailable() throws InterruptedException {
      Circle indicator = lookup("#upgradeIndicator").queryAs(Circle.class);

      // Wait for check to complete then verify indicator is not visible
      TestGuiSetup.waitForCondition(() -> !indicator.isVisible(), 5000);
    }
  }
}
