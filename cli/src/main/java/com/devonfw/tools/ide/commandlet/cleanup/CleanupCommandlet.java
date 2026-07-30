package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.step.Step;

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
  protected void doRun() {

    LOG.debug("Start cleanup commandlet");

    // Identify and remove unused tools.
    Step step = context.newStep("Identify and remove unused software");
    step.run(this::discoverAndDeleteUnusedSoftware);

    LOG.debug("Finished cleanup commandlet");
  }

  /**
   * This method specifies the primary flow for the discovery of installed and unused software, and its deletion.
   */
  private void discoverAndDeleteUnusedSoftware() {
    // Iterate over software in $IDE_ROOT/_ide/software repositories and save installed software to a list
    List<InstalledSoftwareTool> installedSoftwareTools = discoverInstalledSoftware();

    // Scan for IDEasy projects
    List<Path> ideasyProjects = this.context.getFileAccess().listChildren(this.context.getIdeRoot(), Files::isDirectory);

    // Iterate through IDEasy projects and scan software in software folder. Save found software to list
    for (Path ideasyProject : ideasyProjects) {
      if (ideasyProject.getFileName().toString().equals("_ide")) {
        continue;
      }
      Path ideasyProjectSoftware = ideasyProject.resolve(IdeContext.FOLDER_SOFTWARE);
      discoverUsedSoftware(installedSoftwareTools, ideasyProjectSoftware, ideasyProject.getFileName().toString());
      discoverUsedSoftware(installedSoftwareTools, ideasyProjectSoftware.resolve(IdeContext.FOLDER_EXTRA), ideasyProject.getFileName().toString());
    }

    // Mark unused software for deletion
    markUnusedSoftwareForDeletion(installedSoftwareTools);
    // Log summary report and proceed with deletion if user confirms
    logSoftwareToBeDeleted(installedSoftwareTools);
  }

  /**
   * This method discovers all installed tools in all repositories below $IDE_ROOT/_ide/software and saves them to the list of installed tools.
   *
   * @return the list of discovered installed tools.
   */
  private List<InstalledSoftwareTool> discoverInstalledSoftware() {
    List<InstalledSoftwareTool> installedSoftwareTools = new ArrayList<>();
    Path softwareRepositoryPath = this.context.getSoftwareRepositoryPath();
    List<Path> repositoryFolders = this.context.getFileAccess().listChildren(softwareRepositoryPath, Files::isDirectory);
    for (Path repositoryFolder : repositoryFolders) {
      discoverInstalledSoftwareRepository(installedSoftwareTools, repositoryFolder);
    }
    return installedSoftwareTools;
  }

  /**
   * This method discovers all installed tools in one software repository. Installed editions are then recursively discovered.
   *
   * @param installedSoftwareTools the list to populate with discovered tools.
   * @param repositoryFolder The software repository folder to scan.
   */
  private void discoverInstalledSoftwareRepository(List<InstalledSoftwareTool> installedSoftwareTools, Path repositoryFolder) {
    List<Path> toolFolders = this.context.getFileAccess().listChildren(repositoryFolder, Files::isDirectory);
    for (Path toolFolder : toolFolders) {
      InstalledSoftwareTool tool = new InstalledSoftwareTool(toolFolder.getFileName().toString(), this.context.getFileAccess().toRealPath(toolFolder));
      installedSoftwareTools.add(tool);
      discoverInstalledEditions(toolFolder, tool);
    }
  }

  /**
   * This method discovers all installed editions of a tool at $IDE_ROOT/_ide/software/<repository>/<tool> and saves them to the edition list of the tool.
   * Installed versions of the edition are then recursively discovered.
   *
   * @param editionFolder The folder where the editions are saved in ($IDE_ROOT/_ide/software/<repository>/<tool>).
   * @param tool The respective tool for which we are discovering editions.
   */
  private void discoverInstalledEditions(Path editionFolder, InstalledSoftwareTool tool) {
    List<Path> subfolders = this.context.getFileAccess().listChildren(editionFolder, Files::isDirectory);
    for (Path subfolder : subfolders) {
      InstalledSoftwareEdition edition = new InstalledSoftwareEdition(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
      tool.getEditions().add(edition);
      discoverInstalledVersions(subfolder, edition);
    }
  }

  /**
   * This method discovers all installed versions of an edition of a tool at $IDE_ROOT/_ide/software/<repository>/<tool>/<edition> and saves them to the version
   * list of the edition.
   *
   * @param versionFolder The folder where the versions are saved in ($IDE_ROOT/_ide/software/<repository>/<tool>/<edition>).
   * @param edition The respective edition for which we are discovering versions.
   */
  private void discoverInstalledVersions(Path versionFolder, InstalledSoftwareEdition edition) {
    List<Path> subfolders = this.context.getFileAccess().listChildren(versionFolder, Files::isDirectory);
    for (Path subfolder : subfolders) {
      InstalledSoftwareVersion version = new InstalledSoftwareVersion(subfolder.getFileName().toString(),
          this.context.getFileAccess().toRealPath(subfolder));
      edition.getVersions().add(version);
    }
  }

  /**
   * This method scans the software folder of an IDEasy project for installed tools and matches these against the global tool list created earlier. Identified
   * tools are marked as used in the global tool list.
   *
   * @param installedSoftwareTools the list of installed tools to check against.
   * @param softwareFolder The software folder of the IDEasy project to scan for used software.
   * @param projectName The name of the project we are currently scanning.
   */
  private void discoverUsedSoftware(List<InstalledSoftwareTool> installedSoftwareTools, Path softwareFolder, String projectName) {
    discoverUsedSoftware(installedSoftwareTools, softwareFolder, projectName, 0);
  }

  /**
   * This method scans the software folder of an IDEasy project recursively for installed tools and matches these against the global tool list created earlier.
   *
   * @param installedSoftwareTools the list of installed tools to check against.
   * @param softwareFolder The software folder of the IDEasy project to scan for used software.
   * @param projectName The name of the project we are currently scanning.
   * @param depth The current recursion depth.
   */
  private void discoverUsedSoftware(List<InstalledSoftwareTool> installedSoftwareTools, Path softwareFolder, String projectName, int depth) {
    // Get all installed tools for this project
    List<Path> subfolders = this.context.getFileAccess().listChildren(softwareFolder, Files::isDirectory);
    for (Path currentFolder : subfolders) {
      // Converts the path of the tool installation to the real path by eliminating symlinks. This allows us to determine
      // whether a tool is installed locally for an IDEasy project or is part
      // of the global software installation under $IDE_ROOT/_ide/software
      Path referencedPath = this.context.getFileAccess().toRealPath(currentFolder);
      // Check if the resolved project software path belongs to a discovered global software version.
      boolean matchingVersionFound = markMatchingVersionAsUsed(installedSoftwareTools, referencedPath, projectName);
      // For ide-extra-tools.json the actual software link may be located at software/extra/<tool>/<name>.
      if (!matchingVersionFound && depth < 1 && !Files.isSymbolicLink(currentFolder)) {
        discoverUsedSoftware(installedSoftwareTools, currentFolder, projectName, depth + 1);
      }
    }
  }

  /**
   * Marks the installed version containing the referenced project software path as used. The referenced path may point directly to a version folder or to one
   * of its subfolders.
   *
   * @param installedSoftwareTools the list of installed tools to check against.
   * @param referencedPath The resolved path referenced by the IDEasy project.
   * @param projectName The name of the project we are currently scanning.
   * @return {@code true} if a matching installed version was found.
   */
  private boolean markMatchingVersionAsUsed(List<InstalledSoftwareTool> installedSoftwareTools, Path referencedPath, String projectName) {
    for (InstalledSoftwareTool tool : installedSoftwareTools) {
      for (InstalledSoftwareEdition edition : tool.getEditions()) {
        for (InstalledSoftwareVersion version : edition.getVersions()) {
          Path versionPath = version.getPath();
          if (referencedPath.equals(versionPath) || referencedPath.startsWith(versionPath)) {
            version.addUsedBy(projectName);
            return true;
          }
        }
      }
    }
    return false;
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
   * Generates a summary report for tools, editions, and versions to be deleted and prompts the user for confirmation. If the user agrees, we proceed with
   * deletion of the unused tools, editions, and versions.
   *
   * @param installedSoftwareTools the list of installed tools with deletion flags set.
   */
  private void logSoftwareToBeDeleted(List<InstalledSoftwareTool> installedSoftwareTools) {
    String logOutput = "";
    int totalToolsDeleted = 0;
    int totalEditionsDeleted = 0;
    int totalVersionsDeleted = 0;
    for (InstalledSoftwareTool tool : installedSoftwareTools) {
      String logOutputEdition = "";
      int editionsDeleted = 0;
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
          // If at least one version of the edition should be deleted
          if (versionsDeleted < edition.getVersions().size()) {
            logOutputVersion += "\t\t + " + (edition.getVersions().size() - versionsDeleted) + " more version(s) of this edition will not be deleted\n";
          }
          logOutputEdition += "\t - " + edition.getName() + "\n" + logOutputVersion;
          editionsDeleted++;
          totalEditionsDeleted++;
        }
      }
      if (!logOutputEdition.isBlank()) {
        // If at least one edition of the tool should have a delete operation
        if (editionsDeleted < tool.getEditions().size()) {
          logOutputEdition += "\t + " + (tool.getEditions().size() - editionsDeleted) + " more edition(s) of this tool will not be deleted\n";
        }
        logOutput += " - " + tool.getName() + "\n" + logOutputEdition;
        totalToolsDeleted++;
      }
    }

    if (logOutput.isBlank()) {
      LOG.info("No installed tools will be deleted. All installed software is used by at least one project.");
    } else {
      LOG.info("The following installed tools will be deleted: \n" + logOutput);
      LOG.info("Summary: {} installed tool versions across {} editions of {} tools will be deleted.", totalVersionsDeleted, totalEditionsDeleted,
          totalToolsDeleted);

      // Ask for confirmation. Automatically confirmed if the global force option is provided.
      try {
        this.context.askToContinue("Do you want to continue?");

      } catch (CliAbortException e) {
        LOG.info("Installed Tools will not be deleted.");
        return;
      }
      deleteUnusedSoftware(installedSoftwareTools);
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

    // Log completion message
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
      LOG.error("Failed to delete {}: {}", path, e.getMessage());
      return 1;
    }
    return 0;
  }
}
