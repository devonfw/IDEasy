package com.devonfw.ide.gui;

import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Centralizes test/development configuration overrides via environment variables and system properties.
 * <p>
 * Supported overrides:
 * <ul>
 * <li>{@code IDE_VERSION}: Override the IDEasy version for testing version checks/upgrades</li>
 * <li>{@code IDE_UPDATE_AVAILABLE}: Set to "true" to force update availability (for testing update flows)</li>
 * </ul>
 * <p>
 * Usage: {@code IDE_UPDATE_AVAILABLE=true IDE_VERSION=1.0.0 java -jar ideasy-gui.jar}
 * Or: {@code java -DIDE_UPDATE_AVAILABLE=true -DIDE_VERSION=1.0.0 -jar ideasy-gui.jar}
 */
public class TestGuiConfiguration {

  private TestGuiConfiguration() {
    // Utility class, no instances
  }

  /**
   * Applies all test/development configuration overrides from environment variables and system properties.
   * Call this early in the application lifecycle (before creating controllers).
   */
  public static void applyConfigOverrides() {
    applyVersionOverride();
    applyUpdateAvailabilityOverride();
  }

  /**
   * Applies IDE_VERSION override. If set, causes version checks and upgrades to use the mocked version.
   */
  private static void applyVersionOverride() {
    String versionOverride = System.getenv("IDE_VERSION");
    if ((versionOverride == null) || versionOverride.isBlank()) {
      versionOverride = System.getProperty("IDE_VERSION");
    }
    if ((versionOverride != null) && !versionOverride.isBlank()) {
      IdeVersion.setMockVersionForTesting(versionOverride.trim());
    }
  }

  /**
   * Applies IDE_UPDATE_AVAILABLE override. If set to "true", update checks will report that an update is available
   * (useful for testing the update flow without requiring actual updates to be present).
   */
  private static void applyUpdateAvailabilityOverride() {
    String updateOverride = System.getenv("IDE_UPDATE_AVAILABLE");
    if ((updateOverride == null) || updateOverride.isBlank()) {
      updateOverride = System.getProperty("IDE_UPDATE_AVAILABLE");
    }
    if ((updateOverride != null) && !updateOverride.isBlank()) {
      boolean isAvailable = "true".equalsIgnoreCase(updateOverride.trim());
      UpdateController.setMockUpdateAvailable(isAvailable);
    }
  }
}

