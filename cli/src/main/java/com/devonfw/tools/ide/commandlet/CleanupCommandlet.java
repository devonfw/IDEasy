package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.commandlet.cleanupUtil.CleanupIdeTool;
import com.devonfw.tools.ide.commandlet.cleanupUtil.CleanupIdeToolEdition;
import com.devonfw.tools.ide.commandlet.cleanupUtil.CleanupIdeToolEditionVersion;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Commandlet which scans your IDE installation for unused software (tools not currently used by any project) and removes them.
 */
public class CleanupCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

  public final FlagProperty forceDelete;

  public CleanupCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.forceDelete = add(new FlagProperty("--force-delete"));   // Skips confirmation prompts if provided
  }

  @Override
  public String getName() {

    return "cleanup";
  }

  @Override
  protected void doRun() {

    LOG.debug("Start cleanup commandlet");

    List<CleanupIdeTool> installedCleanupIdeTools = new ArrayList<>();

    // Identify and remove unused tools.
    Step step = context.newStep("Identify and remove unused software");
    step.run(() -> discoverAndDeleteUnusedSoftware(installedCleanupIdeTools));

    LOG.debug("Finished cleanup commandlet");
  }

  /**
   * This method specifies the primary flow for the discovery of installed and unused software, and its deletion.
   *
   * @param installedCleanupIdeTools the mutable list of discovered tools, populated and updated during this method's execution.
   */
  private void discoverAndDeleteUnusedSoftware(List<CleanupIdeTool> installedCleanupIdeTools) {
    // Iterate over software in $IDE_ROOT/_ide/software/default folder and save installed software to a list
    discoverInstalledSoftware(installedCleanupIdeTools, this.context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT));

    // Scan for IDEasy projects
    List<Path> ideasyProjects = this.context.getFileAccess().listChildren(this.context.getIdeRoot(), Files::isDirectory);

    // Iterate through IDEasy projects and scan software in software folder. Save found software to list
    for (Path ideasyProject : ideasyProjects) {
      if (ideasyProject.getFileName().toString().equals("_ide")) {
        continue;
      }
      Path ideasyProjectSoftware = ideasyProject.resolve(IdeContext.FOLDER_SOFTWARE);
      discoverUsedSoftware(installedCleanupIdeTools, ideasyProjectSoftware, ideasyProject.getFileName().toString());
      discoverUsedSoftware(installedCleanupIdeTools, ideasyProjectSoftware.resolve(IdeContext.FOLDER_EXTRA), ideasyProject.getFileName().toString());
    }

    // Mark unused software for deletion
    markUnusedSoftwareForDeletion(installedCleanupIdeTools);
    // Log summary report and proceed with deletion if user confirms
    logSoftwareToBeDeleted(installedCleanupIdeTools);
  }

  /**
   * This method discovers all installed tools at $IDE_ROOT/_ide/software/default and saves them to the list of installed tools.
   * Installed editions are then recursively discovered.
   *
   * @param installedCleanupIdeTools the list to populate with discovered tools.
   * @param softwareFolder The folder where the software is saved in ($IDE_ROOT/_ide/software/default).
   */
  private void discoverInstalledSoftware(List<CleanupIdeTool> installedCleanupIdeTools, Path softwareFolder) {
    List<Path> subfolders = this.context.getFileAccess().listChildren(softwareFolder, Files::isDirectory);
    for (Path subfolder : subfolders) {
      CleanupIdeTool tool = new CleanupIdeTool(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
      installedCleanupIdeTools.add(tool);
      discoverInstalledEditions(subfolder, tool);
    }
  }

  /**
   * This method discovers all installed editions of a tool at $IDE_ROOT/_ide/software/default/<tool> and saves them to the edition list of the tool.
   * Installed versions of the edition are then recursively discovered.
   *
   * @param editionFolder The folder where the editions are saved in ($IDE_ROOT/_ide/software/default/<tool>).
   * @param tool The respective tool for which we are discovering editions.
   */
  private void discoverInstalledEditions(Path editionFolder, CleanupIdeTool tool) {
    List<Path> subfolders = this.context.getFileAccess().listChildren(editionFolder, Files::isDirectory);
    for (Path subfolder : subfolders) {
      CleanupIdeToolEdition edition = new CleanupIdeToolEdition(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
      tool.getEditions().add(edition);
      discoverInstalledVersions(subfolder, edition);
    }
  }

  /**
   * This method discovers all installed versions of an edition of a tool at $IDE_ROOT/_ide/software/default/<tool>/<edition> and saves them to the version list of the edition.
   *
   * @param versionFolder The folder where the versions are saved in ($IDE_ROOT/_ide/software/default/<tool>/<edition>).
   * @param edition The respective edition for which we are discovering versions.
   */
  private void discoverInstalledVersions(Path versionFolder, CleanupIdeToolEdition edition) {
    List<Path> subfolders = this.context.getFileAccess().listChildren(versionFolder, Files::isDirectory);
    for (Path subfolder : subfolders) {
      CleanupIdeToolEditionVersion version = new CleanupIdeToolEditionVersion(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
      edition.getVersions().add(version);
    }
  }

  /**
   * This method scans the software folder of an IDEasy project for installed tools and matches these against the global tool list created earlier.
   * Identified tools are marked as used in the global tool list.
   *
   * @param installedCleanupIdeTools the list of installed tools to check against.
   * @param softwareFolder The software folder of the IDEasy project to scan for used software.
   * @param projectName The name of the project we are currently scanning.
   */
  private void discoverUsedSoftware(List<CleanupIdeTool> installedCleanupIdeTools, Path softwareFolder, String projectName) {
    // Get all installed tools for this project
    List<Path> subfolders = this.context.getFileAccess().listChildren(softwareFolder, Files::isDirectory);
    for (Path currentFolder : subfolders) {
      // Converts the path of the tool installation to the real path by eliminating symlinks. This allows us to determine whether a tool is installed locally for an IDEasy project or is part
      // of the global software installation under $IDE_ROOT/_ide/software/default
      currentFolder = this.context.getFileAccess().toRealPath(currentFolder);
      // Check if directory contains a software version file. If so, read version and add to list of used software.
      if (Files.exists(currentFolder.resolve(IdeContext.FILE_SOFTWARE_VERSION))
          || Files.exists(currentFolder.resolve(IdeContext.FILE_LEGACY_SOFTWARE_VERSION))) {
        if (!currentFolder.startsWith(this.context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT))) {
          // We found a software that is locally installed in an IDEasy project but not in the global software folder. We leave these alone.
          continue;
        }
        // Get details of the tool (name, edition, version)
        // currentFolder has the structure «repo-path»/«tool»/«edition»/«version»
        String toolVersion = currentFolder.getFileName().toString();
        Path toolEditionFolder = currentFolder.getParent();
        String toolEdition = toolEditionFolder.getFileName().toString();
        String toolName = toolEditionFolder.getParent().getFileName().toString();
        // Check if software exists in global CleanupIdeTool list. If so, mark as used
        for (CleanupIdeTool tool : installedCleanupIdeTools) {
          if (tool.toolName.equals(toolName)) {
            tool.getUsedBy().add(projectName);
            for (CleanupIdeToolEdition edition : tool.getEditions()) {
              if (edition.editionName.equals(toolEdition)) {
                edition.getUsedBy().add(projectName);
                for (CleanupIdeToolEditionVersion version : edition.getVersions()) {
                  if (version.versionName.equals(toolVersion)) {
                    version.getUsedBy().add(projectName);
                    break;
                  }
                }
                break;
              }
            }
            break;
          }
        }
      }
    }
  }

  /**
   * Sets the delete flag for all unused tools, editions, and versions to true.
   *
   * @param installedCleanupIdeTools the list of installed tools to mark.
   */
  private void markUnusedSoftwareForDeletion(List<CleanupIdeTool> installedCleanupIdeTools) {
    for (CleanupIdeTool tool : installedCleanupIdeTools) {
      for (CleanupIdeToolEdition edition : tool.getEditions()) {
        for (CleanupIdeToolEditionVersion version : edition.getVersions()) {
          if (version.isUnused()) {
            version.setDelete(true);
          }
        }
        if (edition.isUnused()) {
          edition.setDelete(true);
        }
      }
      if (tool.isUnused()) {
        tool.setDelete(true);
      }
    }
  }

  /**
   * Generates a summary report for tools, editions, and versions to be deleted and prompts the user for confirmation.
   * If the user agrees, we proceed with deletion of the unused tools, editions, and versions.
   *
   * @param installedCleanupIdeTools the list of installed tools with deletion flags set.
   */
  private void logSoftwareToBeDeleted(List<CleanupIdeTool> installedCleanupIdeTools) {
    String logOutput = "";
    int totalToolsDeleted = 0;
    int totalEditionsDeleted = 0;
    int totalVersionsDeleted = 0;
    for (CleanupIdeTool tool : installedCleanupIdeTools) {
      String logOutputEdition = "";
      int editionsDeleted = 0;
      for (CleanupIdeToolEdition edition : tool.getEditions()) {
        String logOutputVersion = "";
        int versionsDeleted = 0;
        for (CleanupIdeToolEditionVersion version : edition.getVersions()) {
          if (version.isDelete()) {
            logOutputVersion += "\t\t - " + version.versionName + "\n";
            versionsDeleted++;
            totalVersionsDeleted++;
          }
        }
        if (!logOutputVersion.isBlank()) {
          // If at least one version of the edition should be deleted
          if (versionsDeleted < edition.getVersions().size()) {
            logOutputVersion += "\t\t + " + (edition.getVersions().size() - versionsDeleted) + " more version(s) of this edition will not be deleted\n";
          }
          logOutputEdition += "\t - " + edition.editionName + "\n" + logOutputVersion;
          editionsDeleted++;
          totalEditionsDeleted++;
        }
      }
      if (!logOutputEdition.isBlank()) {
        // If at least one edition of the tool should have a delete operation
        if (editionsDeleted < tool.getEditions().size()) {
          logOutputEdition += "\t + " + (tool.getEditions().size() - editionsDeleted) + " more edition(s) of this tool will not be deleted\n";
        }
        logOutput += " - " + tool.toolName + "\n" + logOutputEdition;
        totalToolsDeleted++;
      }
    }

    if (logOutput.isBlank()) {
      LOG.info("No installed tools will be deleted. All installed software is used by at least one project.");
    } else {
      LOG.info("The following installed tools will be deleted: \n" + logOutput);
      LOG.info("Summary: {} installed tool versions across {} editions of {} tools will be deleted.", totalVersionsDeleted, totalEditionsDeleted, totalToolsDeleted);

      // Ask for confirmation. Skipped if --force-delete flag is provided
      if (!this.forceDelete.isTrue()) {
        try {
          this.context.askToContinue("Do you want to continue?");

        } catch (CliAbortException e) {
          LOG.info("Installed Tools will not be deleted.");
          return;
        }
      }
      deleteUnusedSoftware(installedCleanupIdeTools);
    }
  }

  /**
   * Deletes tools, editions, and versions marked for deletion.
   *
   * @param installedCleanupIdeTools the list of installed tools to delete from.
   */
  private void deleteUnusedSoftware(List<CleanupIdeTool> installedCleanupIdeTools) {
    int failedDeletion = 0;
    // Delete the tool
    for (CleanupIdeTool tool : installedCleanupIdeTools) {
      if (tool.isDelete()) {
        LOG.debug("Deleting tool {} and all its editions and versions in {}", tool.toolName, tool.getPath());
        failedDeletion += deleteFolder(tool.getPath());
        continue;
      }
      // Delete editions of the tool
      for (CleanupIdeToolEdition edition : tool.getEditions()) {
        if (edition.isDelete()) {
          LOG.debug("Deleting edition {} of tool {} and all its versions in {}", edition.editionName, tool.toolName, edition.getPath());
          failedDeletion += deleteFolder(edition.getPath());
          continue;
        }
        // Delete versions of the edition
        for (CleanupIdeToolEditionVersion version : edition.getVersions()) {
          if (version.isDelete()) {
            LOG.debug("Deleting version {} of edition {} of tool {} in {}", version.versionName, edition.editionName, tool.toolName, version.getPath());
            failedDeletion += deleteFolder(version.getPath());
          }
        }
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
