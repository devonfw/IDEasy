package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.repo.MavenRepository;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to upgrade the version of IDEasy
 */
public class UpgradeCommandlet extends Commandlet {

  private static final VersionIdentifier LATEST_SNAPSHOT = VersionIdentifier.of("*-SNAPSHOT");
  public static final String IDEASY = "ideasy";

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
   * Compares two snapshot versions to determine if the latest is newer. Handles versions in the following formats: - Current version format:
   * "2024.12.002-beta-12_18_02-SNAPSHOT" - Latest version format: "2025.01.001-beta-20250118.022832-8"
   * <p>
   * First compares base versions (e.g. 2024.12.002 with 2025.01.001), then timestamps if base versions are equal. Returns false if version formats are
   * unexpected to avoid unintended upgrades.
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

    String version = IdeVersion.get();
    if (IdeVersion.VERSION_UNDEFINED.equals(version)) {
      this.context.warning("You are using IDEasy version {} what indicates local development - skipping upgrade.", version);
      return;
    }
    VersionIdentifier currentVersion = VersionIdentifier.of(version);
    MavenRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier configuredVersion;
    if (version.contains("SNAPSHOT")) {
      configuredVersion = LATEST_SNAPSHOT;
    } else {
      configuredVersion = VersionIdentifier.LATEST;
    }
    this.context.debug("Trying to determine the latest version of IDEasy ({})", configuredVersion);
    VersionIdentifier resolvedVersion = mavenRepo.resolveVersion(IDEASY, IDEASY, configuredVersion);

    boolean upgradeAvailable = resolvedVersion.isGreater(currentVersion);
    if (upgradeAvailable) {
      this.context.info("Upgrading IDEasy from version {} to {}", version, resolvedVersion);
      try {
        this.context.info("Downloading new version...");
        Path downloadTarget = mavenRepo.download(IDEASY, IDEASY, resolvedVersion);
        Path extractionTarget = this.context.getIdeRoot().resolve(IdeContext.FOLDER_IDE);
        if (this.context.getSystemInfo().isWindows()) {
          handleUpgradeOnWindows(downloadTarget, extractionTarget);
        } else {
          this.context.info("Extracting files...");
          this.context.getFileAccess().extract(downloadTarget, extractionTarget);
          this.context.success("Successfully upgraded to version {}", resolvedVersion);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to upgrade version.", e);
      }
    } else {
      this.context.info("Your have IDEasy {} installed what is already the latest version.", version);
    }
  }

  private void handleUpgradeOnWindows(Path downloadTarget, Path extractionTarget) throws IOException {

    ProcessContext pc = this.context.newProcess().executable("bash")
        .addArgs("-c",
            "'sleep 10;tar xvfz \"" + WindowsPathSyntax.MSYS.format(downloadTarget) + "\" -C \"" + WindowsPathSyntax.MSYS.format(extractionTarget) + "\"'");
    pc.run(ProcessMode.BACKGROUND_SILENT);
    this.context.interaction("To prevent windows file locking errors, "
        + "we perform an asynchronous upgrade in background now.\n"
        + "Please wait a minute for the upgrade to complete before running IDEasy commands.");
  }
}
