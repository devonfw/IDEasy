package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
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

  /** Group id of IDEasy. */
  private static final String IDEASY_GROUP_ID = "com.devonfw.tools.IDEasy";

  /** Artifact Id of IDEasy. */
  private static final String IDEASY_ARTIFACT_ID = "ide-cli";

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

  /**
   * Compares two snapshot versions to determine if the latest is newer. Handles versions in the following formats: -
   * Current version format: "2024.12.002-beta-12_18_02-SNAPSHOT" - Latest version format:
   * "2025.01.001-beta-20250118.022832-8"
   *
   * First compares base versions (e.g. 2024.12.002 with 2025.01.001), then timestamps if base versions are equal.
   * Returns false if version formats are unexpected to avoid unintended upgrades.
   *
   * @param currentVersion The current snapshot version
   * @param latestVersion The latest snapshot version to compare against
   * @return true if latestVersion is newer than currentVersion, false otherwise or if formats are invalid
   */
  protected boolean isSnapshotNewer(String currentVersion, String latestVersion) {

    try {
      // Validate input formats
      if (currentVersion == null || latestVersion == null || !currentVersion.contains("-") || !latestVersion.contains(
          "-")) {
        return false;
      }

      // First compare base versions (2024.12.002 with 2025.01.001)
      String currentBase = currentVersion.substring(0, currentVersion.indexOf('-'));
      String latestBase = latestVersion.substring(0, latestVersion.indexOf('-'));

      VersionIdentifier currentBaseVersion = VersionIdentifier.of(currentBase);
      VersionIdentifier latestBaseVersion = VersionIdentifier.of(latestBase);

      // If base versions are different, use regular version comparison
      if (!currentBaseVersion.compareVersion(latestBaseVersion).isEqual()) {
        return currentBaseVersion.compareVersion(latestBaseVersion).isLess();
      }

      // Validate timestamp formats
      String[] currentParts = currentVersion.split("-");
      String[] latestParts = latestVersion.split("-");
      if (currentParts.length < 3 || latestParts.length < 3) {
        return false;
      }

      // Extract timestamps
      String currentTimestamp = currentParts[2].split("-")[0].replace("_", ""); // "010102"
      String[] latestTimestampParts = latestParts[2].split("\\.");
      if (latestTimestampParts.length < 1) {
        return false;
      }

      // Get year from base version (2024.12.002 -> 2024)
      String year = currentBase.substring(0, 4);

      // Parse current date/time using extracted year (currentTimestamp format: MMDDXX)
      LocalDateTime currentTime = LocalDateTime.parse(year + currentTimestamp + "00", // YYYYMMDDHHmm
          DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

      // Parse latest date/time (format: YYYYMMDD.HHMMSS)
      LocalDateTime latestTime = LocalDateTime.parse(latestTimestampParts[0] + "000000",
          DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      return latestTime.isAfter(currentTime);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compare current and latest Snapshot.", e);
    }
  }

  @Override
  public void run() {

    ToolRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier currentVersion = VersionIdentifier.of(IdeVersion.get());
    VersionIdentifier latestVersion = mavenRepo.resolveVersion(IDEASY_GROUP_ID, IDEASY_ARTIFACT_ID, currentVersion);

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
        SystemInfo sys = this.context.getSystemInfo();
        String classifier = sys.getOs() + "-" + sys.getArchitecture();

        Path downloadTarget = mavenRepo.download(IDEASY_GROUP_ID,
            IDEASY_ARTIFACT_ID,
            latestVersion,
            classifier,
            ".tar.gz");

        Path extractionTarget = this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE);
        if (this.context.getSystemInfo().isWindows()) {
          handleWindowsUpgrade(downloadTarget, extractionTarget);
        } else {
          this.context.info("Extracting files...");
          this.context.getFileAccess().extract(downloadTarget, extractionTarget);
          this.context.success("Successfully upgraded to version {}", latestVersion);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to upgrade version.", e);
      }
    } else {
      this.context.info("You are already on the latest version.");
    }
  }

  private void handleWindowsUpgrade(Path downloadTarget, Path extractionTarget) throws IOException {

    Path scriptPath = createUpgradeScriptForWindows();
    ProcessContext pc = this.context.newProcess()
        .errorHandling(ProcessErrorHandling.LOG_WARNING)
        .executable(scriptPath)
        .addArgs(downloadTarget, extractionTarget);
    pc.run(ProcessMode.BACKGROUND_SILENT);
    this.context.success("Upgrade process has been initiated.");
  }

  private Path createUpgradeScriptForWindows() throws IOException {

    Path scriptPath = this.context.getIdeRoot().resolve("upgrade.bat");
    String scriptContent = "@echo off\n" +
        "ping -n 4 127.0.0.1 > nul\n" +
        "C:\\Windows\\System32\\tar.exe -xzf \"%~1\" -C \"%~2\"\n" +
        "del \"%~f0\"";
    Files.write(scriptPath, scriptContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return scriptPath;
  }
}
