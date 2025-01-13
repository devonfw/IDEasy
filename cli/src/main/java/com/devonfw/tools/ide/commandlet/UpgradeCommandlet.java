package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
      LocalDateTime currentTime = LocalDateTime.parse("20" + currentTimestamp.substring(0, 2) + // year
              currentTimestamp.substring(2, 4) + // month
              currentTimestamp.substring(4, 6) + // day
              "000000", // we don't have time in current version
          DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      LocalDateTime latestTime = LocalDateTime.parse(latestTimestamp, DateTimeFormatter.ofPattern("yyyyMMdd"));

      return latestTime.isAfter(currentTime);
    } catch (Exception e) {
      this.context.warning("Failed to parse snapshot versions for comparison", e);
      return true; // If parsing fails, assume we need to upgrade
    }
  }

  @Override
  public void run() {

    ToolRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier currentVersion = VersionIdentifier.of(IdeVersion.get());
    currentVersion = VersionIdentifier.of("2024.12.001-beta");
    VersionIdentifier latestVersion = mavenRepo.resolveVersion(IDEASY_GROUP, IDEASY_ARTIFACT, null);

    boolean upgradeAvailable;
    if (currentVersion.toString().contains("SNAPSHOT")) {
      upgradeAvailable = isSnapshotNewer(currentVersion.toString(), latestVersion.toString());
    } else {
      VersionComparisonResult comparisonResult = currentVersion.compareVersion(latestVersion);
      upgradeAvailable = comparisonResult.isLess();
    }

    if (upgradeAvailable) {
      this.context.info("A newer version is available: " + latestVersion);
      try {
        this.context.info("Downloading new version...");
        Path downloadTarget = mavenRepo.download(IDEASY_GROUP, IDEASY_ARTIFACT, latestVersion);

        if (this.context.getSystemInfo().isWindows()) {
          // Windows-specific handling
          Path scriptPath = createWindowsUpgradeScript(downloadTarget);

          // Start the upgrade script in the background
          ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING)
              .executable(scriptPath);

          pc.run(ProcessMode.BACKGROUND_SILENT);

          this.context.success("Upgrade script has been initiated. The application will restart shortly.");
          System.exit(0);
        } else {
          // Normal extraction for non-Windows systems
          this.context.info("Extracting files...");
          FileAccess fileAccess = this.context.getFileAccess();
          fileAccess.extract(downloadTarget, this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE));
          this.context.success("Successfully upgraded to version " + latestVersion);
        }
      } catch (Exception e) {
        this.context.error("Failed to upgrade: " + e.getMessage());
      }
    } else {
      this.context.info("You are already on the latest version.");
    }
  }

  private Path createWindowsUpgradeScript(Path downloadTarget) throws IOException {
    Path scriptPath = this.context.getIdeRoot().resolve("upgrade.bat");
    String ideFolderPath = this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE).toString();
    // Create the content of the batch script
    String scriptContent = String.format(
        "@echo off\r\n" +
            "timeout /t 3 /nobreak > nul\r\n" +  // Wait for 3 seconds
            "echo Starting upgrade process...\r\n" +
            "tar -xzf \"%s\" -C \"%s\"\r\n" +  // Extract tar.gz file to IDE folder
            "echo Upgrade complete!\r\n" +
            "del \"%%~f0\"\r\n",  // Self-delete the script - note the %% to escape %
        downloadTarget.toString(),
        ideFolderPath
    );

    Files.write(scriptPath, scriptContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return scriptPath;
  }
}
