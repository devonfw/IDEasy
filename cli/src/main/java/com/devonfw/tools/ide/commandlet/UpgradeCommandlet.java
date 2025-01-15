package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.repo.MavenRepository;
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

/**
 * {@link Commandlet} to upgrade the version of IDEasy
 */
public class UpgradeCommandlet extends Commandlet {

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
    try {
      // First compare the base versions
      String currentBase = currentVersion.substring(0, currentVersion.indexOf('-'));
      String latestBase = latestVersion.substring(0, latestVersion.indexOf('-'));

      VersionIdentifier currentBaseVersion = VersionIdentifier.of(currentBase);
      VersionIdentifier latestBaseVersion = VersionIdentifier.of(latestBase);

      // If base versions are different, use regular version comparison
      if (!currentBaseVersion.compareVersion(latestBaseVersion).isEqual()) {
        return currentBaseVersion.compareVersion(latestBaseVersion).isLess();
      }

      // Base versions are equal, now compare timestamps
      String[] currentParts = currentVersion.split("-");
      if (currentParts.length < 3) {
        return true; // If format is unexpected, assume we need to upgrade
      }
      // currentParts[2] format is "MM_DD_HH-SNAPSHOT"
      String currentTimestamp = currentParts[2].split("-")[0].replace("_", ""); // "010802"

      String[] latestParts = latestVersion.split("-");
      if (latestParts.length < 3) {
        return true;
      }

      // Get the timestamp part (20250108.023429)
      String[] latestTimestampParts = latestParts[2].split("\\.");
      String latestDatePart = latestTimestampParts[0]; // "20250108"

      // Convert timestamps to LocalDateTime for comparison
      try {
        // Current: month=01, day=08, hour=02
        LocalDateTime currentTime = LocalDateTime.now()
            .withMonth(Integer.parseInt(currentTimestamp.substring(0, 2)))
            .withDayOfMonth(Integer.parseInt(currentTimestamp.substring(2, 4)))
            .withHour(Integer.parseInt(currentTimestamp.substring(4, 6)))
            .withMinute(0)
            .withSecond(0);

        // Latest: truncate to hours to match current time format
        LocalDateTime latestTime = LocalDateTime.parse(
            latestDatePart + "000000", // Pad with zeros for hour/minute/second
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        ).withMinute(0).withSecond(0);

        return latestTime.isAfter(currentTime);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to parse snapshot version timestamps for comparison.", e);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compare snapshot versions.", e);
    }
  }

  @Override
  public void run() {

    ToolRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier currentVersion = VersionIdentifier.of(IdeVersion.get());
    VersionIdentifier latestVersion = mavenRepo.resolveVersion(MavenRepository.IDEASY_GROUP_ID, MavenRepository.IDEASY_ARTIFACT_ID, null);

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
        Path downloadTarget = mavenRepo.download(MavenRepository.IDEASY_GROUP_ID, MavenRepository.IDEASY_ARTIFACT_ID, latestVersion);
        Path extractionTarget = this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE);
        if (this.context.getSystemInfo().isWindows()) {
          // Windows-specific handling
          Path scriptPath = createUpgradeScript();
          // Start the upgrade script in the background
          ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING)
              .executable(scriptPath).addArgs(downloadTarget, extractionTarget);
          pc.run(ProcessMode.BACKGROUND_SILENT);
          this.context.success("Upgrade script has been initiated. The application will restart shortly.");
        } else {
          // Normal extraction for non-Windows systems
          this.context.info("Extracting files...");
          FileAccess fileAccess = this.context.getFileAccess();
          fileAccess.extract(downloadTarget, extractionTarget);
          this.context.success("Successfully upgraded to version " + latestVersion);
        }
      } catch (Exception e) {
        this.context.error("Failed to upgrade: " + e.getMessage());
      }
    } else {
      this.context.info("You are already on the latest version.");
    }
  }

  private Path createUpgradeScript() throws IOException {

    Path scriptPath = this.context.getIdeRoot().resolve("upgrade.bat");

    String scriptContent = "@echo off\n"
        + "ping -n 4 127.0.0.1 > nul\n"
        + "C:\\Windows\\System32\\tar.exe -xzf \"%~1\" -C \"%~2\"\n"
        + "if errorlevel 1 (exit /b 1)\n"
        + "del \"%~f0\"";

    Files.write(scriptPath, scriptContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return scriptPath;
  }
}
