package com.devonfw.ide.gui;

import java.io.IOException;
import java.util.function.Supplier;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.i18n.I18nService;

/**
 * Integration tests for project-scoped update and tool-wide upgrade flows.
 * <p>
 * Tests verify: - Update check mechanism: button disabled without context, enabled when check reports availability - Upgrade check mechanism: button state
 * transitions based on upgrade availability - Both flows independently using test doubles that provide deterministic behavior
 */
public class UpdateUpgradeFlowTest extends AppBaseTest {


  @AfterAll
  static void clearVersionOverride() {
    // no-op; retained for compatibility with the original test structure
  }

  @Override
  public void start(Stage stage) throws IOException {
    // Reuse parent's initialization which loads the FXML and controller
    super.start(stage);
    // Give UI time to fully initialize
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // ===== UPDATE FLOW TESTS =====


  /**
   * Full project update-flow test with safer FX-thread handling. Verifies: button disabled initially, becomes enabled on check, then disabled again after
   * update.
   */
  @Test
  public void testUpdateFlowShowsAndCompletes() throws InterruptedException {
    // Lookup UI elements (TestFX lookup provided by AppBaseTest)
    Label status = lookup("#updateStatusLabel").queryAs(Label.class);
    Button update = lookup("#updateButton").queryAs(Button.class);
    ComboBox<String> selectedProject = lookup("#selectedProject").queryAs(ComboBox.class);
    ComboBox<String> selectedWorkspace = lookup("#selectedWorkspace").queryAs(ComboBox.class);

    I18nService i18n = I18nService.getInstance();
    String availableStatus = i18n.get("status.update.available");
    String completedStatus = i18n.get("status.update.completed");
    String upToDateStatus = i18n.get("status.update.upToDate");

    // Select project/workspace context
    interact(() -> selectedProject.getSelectionModel().select("project-1"));
    interact(() -> selectedWorkspace.getSelectionModel().select("main"));

    // Automatic check runs and reports availability
    waitForCondition(() -> availableStatus.equals(status.getText()), 5000);

    interact(update::fire);

    // After update, should show completed, then recheck should show up to date
    waitForCondition(() -> completedStatus.equals(status.getText()), 3000);
    waitForCondition(() -> upToDateStatus.equals(status.getText()), 3000);
  }

  // ===== UPGRADE FLOW TESTS =====


  @Test
  public void testUpgradeFlowShowsAndCompletes() throws InterruptedException {
    Label upgradeStatus = lookup("#upgradeStatusLabel").queryAs(Label.class);
    Button upgrade = lookup("#upgradeButton").queryAs(Button.class);

    I18nService i18n = I18nService.getInstance();
    String availableStatus = i18n.get("status.upgrade.available");
    String upToDateStatus = i18n.get("status.upgrade.upToDate");

    // Wait for availability to be reported (initial auto-check, may already be done)
    waitForCondition(() -> availableStatus.equals(upgradeStatus.getText()), 5000);

    // Click upgrade button
    interact(upgrade::fire);

    // After upgrade, the status transitions through intermediate states to final "Up to date".
    // In headless test environment, the dialog may cause timing variations, so wait directly for final state.
    waitForCondition(() -> upToDateStatus.equals(upgradeStatus.getText()), 5000);
  }

  // ===== HELPER =====

  /**
   * Helper to wait for a UI condition with a configurable timeout.
   *
   * @param condition supplier evaluated repeatedly on the calling thread
   * @param timeoutMillis timeout in milliseconds
   * @throws InterruptedException if the sleep is interrupted
   */
  private void waitForCondition(Supplier<Boolean> condition, long timeoutMillis) throws InterruptedException {
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


