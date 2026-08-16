package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.mvn.MvnRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Commandlet which scans your IDE installation for unused software (tools not currently used by any project) and removes them.
 */
public class CleanupCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

  /**
   * Constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CleanupCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
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

    InstalledSoftware installedSoftware = new InstalledSoftware();

    Step step = this.context.newStep("Identify unused software");
    step.run(() -> discoverUnusedSoftware(installedSoftware), true);

    logSoftwareToBeDeleted(installedSoftware.getTools());

    if (hasSoftwareToDelete(installedSoftware.getTools())) {
      this.context.askToContinue("Do you want to continue?");
      deleteUnusedSoftware(installedSoftware.getTools());
    }

    LOG.debug("Finished cleanup commandlet");
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
}
