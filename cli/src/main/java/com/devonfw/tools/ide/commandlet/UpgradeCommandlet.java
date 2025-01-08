package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpgradeCommandlet extends Commandlet {

  private static final String IDEASY_GROUP = "com.devonfw.tools.IDEasy";

  private static final String IDEASY_ARTIFACT = "ide-cli";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpgradeCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "upgrade";
  }

  private boolean isSnapshotNewer(String currentVersion, String latestVersion) {

    // Extract timestamp from current version (format: 2025.01.001-beta-01_08_02-SNAPSHOT)
    String[] currentParts = currentVersion.split("-");
    if (currentParts.length < 3) {
      return true; // If format is unexpected, assume we need to upgrade
    }
    String currentTimestamp = currentParts[2].replace("_", ""); // "010802"

    // Extract timestamp from latest version (format: 2025.01.001-beta-20250108.023429-3)
    String[] latestParts = latestVersion.split("-");
    if (latestParts.length < 3) {
      return true;
    }
    // Get the date part from timestamp (first 8 chars of "20250108.023429")
    String latestTimestamp = latestParts[1].substring(0, 8);

    // Convert timestamps to LocalDateTime for comparison
    try {
      LocalDateTime currentTime = LocalDateTime.parse(
          "20" + currentTimestamp.substring(0, 2) + // year
              currentTimestamp.substring(2, 4) + // month
              currentTimestamp.substring(4, 6) + // day
              "000000", // we don't have time in current version
          DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      LocalDateTime latestTime = LocalDateTime.parse(
          latestTimestamp,
          DateTimeFormatter.ofPattern("yyyyMMdd"));

      return latestTime.isAfter(currentTime);
    } catch (Exception e) {
      this.context.warning("Failed to parse snapshot versions for comparison: " + e.getMessage());
      return true; // If parsing fails, assume we need to upgrade
    }
  }

  @Override
  public void run() {

    ToolRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier currentVersion = VersionIdentifier.of(IdeVersion.get());
    VersionIdentifier latestVersion = mavenRepo.resolveVersion(IDEASY_GROUP, IDEASY_ARTIFACT, null);

    boolean upgradeAvailable;
    if (currentVersion.toString().contains("SNAPSHOT")) {
      // Handle SNAPSHOT versions
      upgradeAvailable = isSnapshotNewer(currentVersion.toString(), latestVersion.toString());
    } else {
      // Handle release versions
      VersionComparisonResult comparisonResult = currentVersion.compareVersion(latestVersion);
      upgradeAvailable = comparisonResult.isLess();
    }

    if (upgradeAvailable) {
      this.context.info("A newer version is available: " + latestVersion);
      try {
        this.context.info("Downloading new version...");
        Path downloadTarget = mavenRepo.download(IDEASY_GROUP, IDEASY_ARTIFACT, latestVersion);

        this.context.info("Extracting files...");
        FileAccess fileAccess = this.context.getFileAccess();
        fileAccess.extract(downloadTarget, this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE));

        this.context.success("Successfully upgraded to version " + latestVersion);
      } catch (Exception e) {
        this.context.error("Failed to upgrade: " + e.getMessage());
      }
    } else {
      this.context.info("You are already on the latest version.");
    }
  }
}
