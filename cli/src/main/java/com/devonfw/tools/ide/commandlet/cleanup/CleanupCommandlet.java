package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.mvn.MvnRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Commandlet which scans your IDE installation for unused software (tools not currently used by any project) and removes them.
 */
public class CleanupCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

  /** The default retention period of stale files. Stale files are considered stale after 1 year (365 days) of inactivity. */
  public static final Duration DEFAULT_RETENTION_DELAY = Duration.ofDays(365);

  /** The {@link StringProperty} of the {@code --retention-delay} option. */
  private final StringProperty retentionDelay;

  /**
   * Constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CleanupCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.retentionDelay = add(new StringProperty("--retention-delay", false, null));
  }

  @Override
  public String getName() {

    return "cleanup";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  protected void doRun() {

    LOG.debug("Start cleanup commandlet");

    Duration retentionDelay = getRetentionDelay();

    InstalledSoftware installedSoftware = new InstalledSoftware();

    Step step = this.context.newStep("Identify unused software");
    step.run(() -> discoverUnusedSoftware(installedSoftware), true);

    logSoftwareToBeDeleted(installedSoftware.getTools());

    List<Path> staleRoots = new ArrayList<>();
    List<Path> staleFiles = new ArrayList<>();
    if (this.context.getIdeHome() != null) {
      Step staleStep = this.context.newStep("Identify stale files");
      staleStep.run(() -> {
        staleRoots.addAll(getStaleFileRoots());
        discoverStaleFiles(staleRoots, staleFiles, retentionDelay);
      }, true);
      logStaleFilesToBeDeleted(staleFiles, retentionDelay);
    }

    boolean hasStaleFiles = !staleFiles.isEmpty();
    if (hasSoftwareToDelete(installedSoftware.getTools()) || hasStaleFiles) {
      this.context.askToContinue("Do you want to continue?");
      deleteUnusedSoftware(installedSoftware.getTools());
      if (hasStaleFiles) {
        deleteStaleFiles(staleFiles, staleRoots);
      }
    }

    LOG.debug("Finished cleanup commandlet");
  }

  /**
   * Determines the retention delay to use.
   *
   * @return the retention delay, {@link #DEFAULT_RETENTION_DELAY} if the {@code --retention-delay} option was not provided.
   * @throws CliException if the provided value is not a valid ISO-8601 time-based duration.
   */
  private Duration getRetentionDelay() {

    String value = this.retentionDelay.getValueAsString();
    if (value == null) {
      return DEFAULT_RETENTION_DELAY;
    }
    try {
      return Duration.parse(value);
    } catch (DateTimeParseException e) {
      throw new CliException(
          "Invalid value '" + value + "' for --retention-delay. Please provide a time-based ISO-8601 duration such as P30D or PT2H30M.",
          e);
    }
  }

  /**
   * Discovers installed and unused software.
   *
   * @param installedSoftware the data structure to populate with installed software.
   */
  private void discoverUnusedSoftware(InstalledSoftware installedSoftware) {

    discoverInstalledSoftware(installedSoftware);

    List<Path> ideasyProjects = this.context.findProjects();
    for (Path ideasyProject : ideasyProjects) {
      String projectName = ideasyProject.getFileName().toString();
      Path ideasyProjectSoftware = ideasyProject.resolve(IdeContext.FOLDER_SOFTWARE);
      discoverUsedSoftware(installedSoftware, ideasyProjectSoftware, projectName, 1);
      discoverUsedSoftware(installedSoftware, ideasyProjectSoftware.resolve(IdeContext.FOLDER_EXTRA), projectName);
    }

    markUnusedSoftwareForDeletion(installedSoftware.getTools());
  }

  /**
   * Discovers all installed tools in the default, Maven, and custom software repositories.
   *
   * @param installedSoftware the data structure to populate with installed software.
   */
  private void discoverInstalledSoftware(InstalledSoftware installedSoftware) {

    Path softwareRepositoryPath = this.context.getSoftwareRepositoryPath();

    discoverInstalledSoftwareRepository(installedSoftware, softwareRepositoryPath.resolve(ToolRepository.ID_DEFAULT));
    discoverInstalledSoftwareRepository(installedSoftware, softwareRepositoryPath.resolve(MvnRepository.ID));

    if (this.context.getSettingsPath() != null) {
      discoverInstalledSoftwareRepository(installedSoftware, softwareRepositoryPath.resolve(this.context.getCustomToolRepository().getId()));
    }
  }

  /**
   * Discovers all installed tools in one software repository. Installed editions are then recursively discovered.
   *
   * @param installedSoftware the data structure to populate with installed software.
   * @param repositoryFolder the software repository folder to scan.
   */
  private void discoverInstalledSoftwareRepository(InstalledSoftware installedSoftware, Path repositoryFolder) {

    if (!Files.isDirectory(repositoryFolder)) {
      return;
    }

    List<Path> toolFolders = this.context.getFileAccess().listChildren(repositoryFolder, Files::isDirectory);
    for (Path toolFolder : toolFolders) {
      Path toolPath = this.context.getFileAccess().toRealPath(toolFolder);
      InstalledSoftwareTool tool = new InstalledSoftwareTool(toolFolder.getFileName().toString(), toolPath);
      installedSoftware.addTool(tool);
      discoverInstalledEditions(installedSoftware, toolFolder, tool);
    }
  }

  /**
   * Discovers all installed editions of the given tool.
   *
   * @param installedSoftware the installed software data structure.
   * @param toolFolder the folder containing the editions of the tool.
   * @param tool the tool to populate with discovered editions.
   */
  private void discoverInstalledEditions(InstalledSoftware installedSoftware, Path toolFolder, InstalledSoftwareTool tool) {

    List<Path> editionFolders = this.context.getFileAccess().listChildren(toolFolder, Files::isDirectory);
    for (Path editionFolder : editionFolders) {
      Path editionPath = this.context.getFileAccess().toRealPath(editionFolder);
      InstalledSoftwareEdition edition = new InstalledSoftwareEdition(editionFolder.getFileName().toString(), editionPath);
      installedSoftware.addEdition(tool, edition);
      discoverInstalledVersions(installedSoftware, editionFolder, edition);
    }
  }

  /**
   * Discovers all installed versions of the given edition.
   *
   * @param installedSoftware the installed software data structure.
   * @param editionFolder the folder containing the versions of the edition.
   * @param edition the edition to populate with discovered versions.
   */
  private void discoverInstalledVersions(InstalledSoftware installedSoftware, Path editionFolder, InstalledSoftwareEdition edition) {

    List<Path> versionFolders = this.context.getFileAccess().listChildren(editionFolder, Files::isDirectory);
    for (Path versionFolder : versionFolders) {
      Path versionPath = this.context.getFileAccess().toRealPath(versionFolder);
      InstalledSoftwareVersion version = new InstalledSoftwareVersion(versionFolder.getFileName().toString(), versionPath);
      installedSoftware.addVersion(edition, version);
    }
  }

  /**
   * Scans the software folder of an IDEasy project for used software.
   *
   * @param installedSoftware the installed software data structure.
   * @param softwareFolder the software folder to scan.
   * @param projectName the name of the project being scanned.
   */
  private void discoverUsedSoftware(InstalledSoftware installedSoftware, Path softwareFolder, String projectName) {

    discoverUsedSoftware(installedSoftware, softwareFolder, projectName, 0);
  }

  /**
   * Scans the software folder of an IDEasy project recursively for used software.
   *
   * @param installedSoftware the installed software data structure.
   * @param softwareFolder the software folder to scan.
   * @param projectName the name of the project being scanned.
   * @param depth the current recursion depth.
   */
  private void discoverUsedSoftware(InstalledSoftware installedSoftware, Path softwareFolder, String projectName, int depth) {

    List<Path> subfolders = this.context.getFileAccess().listChildren(softwareFolder, Files::isDirectory);
    for (Path currentFolder : subfolders) {
      Path referencedPath = this.context.getFileAccess().toRealPath(currentFolder);
      boolean matchingVersionFound = markMatchingVersionAsUsed(installedSoftware, referencedPath, projectName);

      // For ide-extra-tools.json the actual software link may be located at software/extra/<tool>/<name>.
      if (!matchingVersionFound && depth < 1 && !Files.isSymbolicLink(currentFolder)) {
        discoverUsedSoftware(installedSoftware, currentFolder, projectName, depth + 1);
      }
    }
  }

  /**
   * Marks the installed version containing the referenced path as used.
   *
   * @param installedSoftware the installed software data structure.
   * @param referencedPath the resolved path referenced by the IDEasy project.
   * @param projectName the name of the project using the version.
   * @return {@code true} if a matching installed version was found.
   */
  private boolean markMatchingVersionAsUsed(InstalledSoftware installedSoftware, Path referencedPath, String projectName) {

    InstalledSoftwareVersion version = installedSoftware.findVersion(referencedPath);
    if (version == null) {
      return false;
    }

    version.addUsedBy(projectName);
    return true;
  }

  /**
   * Sets the delete flag for all unused software versions to {@code true}.
   *
   * @param installedSoftwareTools the list of installed tools containing the versions to mark.
   */
  private void markUnusedSoftwareForDeletion(List<InstalledSoftwareTool> installedSoftwareTools) {

    for (InstalledSoftwareTool tool : installedSoftwareTools) {
      for (InstalledSoftwareEdition edition : tool.getEditions()) {
        for (InstalledSoftwareVersion version : edition.getVersions()) {
          if (version.isUnused()) {
            version.setDelete(true);
          }
        }
      }
    }
  }

  /**
   * Checks whether at least one installed software version is marked for deletion.
   *
   * @param installedSoftwareTools the installed software to inspect.
   * @return {@code true} if at least one version is marked for deletion.
   */
  private boolean hasSoftwareToDelete(List<InstalledSoftwareTool> installedSoftwareTools) {

    return installedSoftwareTools.stream()
        .flatMap(tool -> tool.getEditions().stream())
        .flatMap(edition -> edition.getVersions().stream())
        .anyMatch(InstalledSoftwareVersion::isDelete);
  }

  /**
   * Logs a summary of the software versions marked for deletion.
   *
   * @param installedSoftwareTools the list of installed tools containing versions with deletion flags.
   */
  private void logSoftwareToBeDeleted(List<InstalledSoftwareTool> installedSoftwareTools) {

    String logOutput = "";
    int totalAffectedTools = 0;
    int totalAffectedEditions = 0;
    int totalVersionsDeleted = 0;

    for (InstalledSoftwareTool tool : installedSoftwareTools) {
      String logOutputEdition = "";

      for (InstalledSoftwareEdition edition : tool.getEditions()) {
        String logOutputVersion = "";
        int versionsDeleted = 0;

        for (InstalledSoftwareVersion version : edition.getVersions()) {
          if (version.isDelete()) {
            logOutputVersion += "\t\t - " + version.getName() + "\n";
            versionsDeleted++;
            totalVersionsDeleted++;
          }
        }

        if (!logOutputVersion.isBlank()) {
          if (versionsDeleted < edition.getVersions().size()) {
            logOutputVersion += "\t\t + " + (edition.getVersions().size() - versionsDeleted) + " more version(s) of this edition will not be deleted\n";
          }
          logOutputEdition += "\t - " + edition.getName() + "\n" + logOutputVersion;
          totalAffectedEditions++;
        }
      }

      if (!logOutputEdition.isBlank()) {
        logOutput += " - " + tool.getName() + "\n" + logOutputEdition;
        totalAffectedTools++;
      }
    }

    if (logOutput.isBlank()) {
      LOG.info("No installed tools will be deleted. All installed software is used by at least one project.");
    } else {
      LOG.info("The following installed tool versions will be deleted: \n" + logOutput);
      LOG.info("Summary: {} installed tool versions across {} affected editions of {} affected tools will be deleted.", totalVersionsDeleted,
          totalAffectedEditions, totalAffectedTools);
    }
  }

  /**
   * Deletes software versions marked for deletion and removes their parent edition and tool folders if they become empty.
   *
   * @param installedSoftwareTools the list of installed tools containing the versions to delete.
   */
  private void deleteUnusedSoftware(List<InstalledSoftwareTool> installedSoftwareTools) {

    int failedDeletion = 0;

    for (InstalledSoftwareTool tool : installedSoftwareTools) {
      for (InstalledSoftwareEdition edition : tool.getEditions()) {
        for (InstalledSoftwareVersion version : edition.getVersions()) {
          if (version.isDelete()) {
            LOG.debug("Deleting version {} of edition {} of tool {} in {}", version.getName(), edition.getName(), tool.getName(), version.getPath());
            failedDeletion += deleteFolder(version.getPath());
          }
        }

        if (isEmptyFolder(edition.getPath())) {
          LOG.debug("Deleting empty edition {} of tool {} in {}", edition.getName(), tool.getName(), edition.getPath());
          failedDeletion += deleteFolder(edition.getPath());
        }
      }

      if (isEmptyFolder(tool.getPath())) {
        LOG.debug("Deleting empty tool {} in {}", tool.getName(), tool.getPath());
        failedDeletion += deleteFolder(tool.getPath());
      }
    }

    if (failedDeletion > 0) {
      LOG.warn("Unused tools have been deleted.\nFailed to delete {} tools/editions/versions. Please check the log for details.", failedDeletion);
    } else {
      IdeLogLevel.SUCCESS.log(LOG, "Unused tools have been deleted successfully.");
    }
  }

  /**
   * Checks whether the given folder exists and has no remaining children.
   *
   * @param folder the folder to check.
   * @return {@code true} if the folder exists and is empty.
   */
  private boolean isEmptyFolder(Path folder) {

    return Files.isDirectory(folder) && this.context.getFileAccess().listChildren(folder, child -> true).isEmpty();
  }

  /**
   * Deletes a folder at a given path. Logs an error message if unsuccessful.
   *
   * @param path The path of the folder to delete.
   * @return 0 if deletion was successful, 1 if deletion failed.
   */
  private int deleteFolder(Path path) {
    try {
      this.context.getFileAccess().delete(path);
    } catch (Exception e) {
      LOG.error("Failed to delete {}.", path, e);
      return 1;
    }
    return 0;
  }

  /**
   * Discovers stale files, i.e. files that have not been modified within the given retention delay, in the given root folders.
   *
   * @param roots the folders to scan.
   * @param staleFiles the list to populate with the stale files.
   * @param retentionDelay the age after which a file is considered stale.
   */
  private void discoverStaleFiles(List<Path> roots, List<Path> staleFiles, Duration retentionDelay) {

    for (Path root : roots) {
      discoverStaleFilesRecursive(root, retentionDelay, staleFiles);
    }
  }

  /**
   * Recursively collects the stale files below the given folder, i.e. all files that are older than the retention delay.
   *
   * @param folder the folder to scan.
   * @param retentionDelay the age after which a file is considered stale.
   * @param staleFiles the list to populate with the stale files.
   */
  private void discoverStaleFilesRecursive(Path folder, Duration retentionDelay, List<Path> staleFiles) {

    if (!Files.isDirectory(folder)) {
      return;
    }

    for (Path child : this.context.getFileAccess().listChildren(folder, child -> true)) {
      if (Files.isDirectory(child)) {
        discoverStaleFilesRecursive(child, retentionDelay, staleFiles);
      } else if (isStale(child, retentionDelay)) {
        staleFiles.add(child);
      }
    }
  }

  /**
   * Determines the folders scanned for stale files: the IDEasy updates folder, the temporary folder, the download cache and the legacy download
   * cache under {@code ~/Downloads/ide}.
   *
   * @return the list of root folders to scan, excluding folders that do not exist.
   */
  private List<Path> getStaleFileRoots() {

    List<Path> roots = new ArrayList<>();

    addStaleFileRoot(roots, this.context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES));
    addStaleFileRoot(roots, this.context.getTempPath());
    Path downloadPath = this.context.getDownloadPath();
    addStaleFileRoot(roots, downloadPath);

    // older versions kept the download cache under ~/Downloads/ide - scan that location too if it is distinct and still exists
    Path legacy = this.context.getUserHome().resolve(IdeContext.FOLDER_DOWNLOADS).resolve("ide");
    if (!legacy.equals(downloadPath)) {
      addStaleFileRoot(roots, legacy);
    }

    return roots;
  }

  /**
   * Adds the given folder to the list of scanned roots if it exists.
   *
   * @param roots the list of root folders to populate.
   * @param root the candidate root folder.
   */
  private void addStaleFileRoot(List<Path> roots, Path root) {

    if (root != null && Files.exists(root)) {
      roots.add(root);
    }
  }

  /**
   * Determines whether the given file is older than the given retention delay.
   *
   * @param file the file to check.
   * @param retentionDelay the age after which the file is considered stale.
   * @return {@code true} if the file exists and is older than the retention delay.
   */
  private boolean isStale(Path file, Duration retentionDelay) {

    Duration age = this.context.getFileAccess().getFileAge(file);
    return (age != null) && age.compareTo(retentionDelay) > 0;
  }

  /**
   * Logs a summary of the stale files to be deleted.
   *
   * @param staleFiles the stale files to report.
   * @param retentionDelay the age that the stale files exceed.
   */
  private void logStaleFilesToBeDeleted(List<Path> staleFiles, Duration retentionDelay) {

    if (staleFiles.isEmpty()) {
      LOG.info("No stale files older than {} will be deleted.", retentionDelay);
    } else {
      for (Path staleFile : staleFiles) {
        LOG.info("\t - {} will be deleted", staleFile);
      }
      LOG.info("Summary: {} stale file(s) older than {} will be deleted.", staleFiles.size(), retentionDelay);
    }
  }

  /**
   * Deletes the given stale files and removes the parent folders that became empty as a result. Folders above the scanned roots are never removed.
   *
   * @param staleFiles the stale files to delete.
   * @param roots the folders that were scanned, which must not be removed themselves.
   */
  private void deleteStaleFiles(List<Path> staleFiles, List<Path> roots) {

    int failedDeletion = 0;

    for (Path staleFile : staleFiles) {
      if (Files.exists(staleFile)) {
        LOG.debug("Deleting stale file {}", staleFile);
        failedDeletion += deleteFolder(staleFile);
      }
    }

    // Remove the folders that became empty after deleting the stale files, walking up from each deleted file but never removing the scanned roots
    // themselves.
    Set<Path> prunedFolders = new HashSet<>();
    for (Path staleFile : staleFiles) {
      Path folder = staleFile.getParent();
      while (folder != null && !prunedFolders.contains(folder) && !roots.contains(folder)) {
        if (!isEmptyFolder(folder)) {
          break;
        }
        LOG.debug("Deleting empty folder {}", folder);
        prunedFolders.add(folder);
        failedDeletion += deleteFolder(folder);
        folder = folder.getParent();
      }
    }

    if (failedDeletion > 0) {
      LOG.warn("Stale files have been deleted.\nFailed to delete {} file(s) or folder(s). Please check the log for details.", failedDeletion);
    } else {
      IdeLogLevel.SUCCESS.log(LOG, "Stale files have been deleted successfully.");
    }
  }
}
