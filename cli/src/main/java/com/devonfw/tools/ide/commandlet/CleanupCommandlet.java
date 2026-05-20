package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.step.Step;

/**
* Commandlet which scans your IDE installation for unused software (tools not currently used by any project) and removes them.
*/
public class CleanupCommandlet extends Commandlet{

    public final FlagProperty forceDelete;

    private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

    /**
    * Class which represents individual IDE tools. Contains multiple parameter such as the name of the tool, the path where it is installed,
    * a list of projects which use this tool and a boolean whether the tool is marked for deletion or not.
    * Furthermore, it contains a list of editions of the edition (e.g. for intellij, we could have the editions "intellij" or "ultimate").
    */
    private static class IdeToolEditionVersion {
        String versionName;
        private final Path path;
        private final List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;

        IdeToolEditionVersion(String versionName, Path path) {
            this.versionName = versionName;
            this.path = path;
        }
    }
    
    /**
    * Class which represents individual editions of an IDE tool. Contains multiple parameter such as the name of the edition, the path where it is installed,
    * a list of projects which use this edition and a boolean whether the edition is marked for deletion or not.
    * Furthermore, it contains a list of versions of the edition (e.g. for intellij ultimate edition, we could have version 2022.3 and 2023.1 installed).
    */
    private static class IdeToolEdition {
        String editionName;
        private final Path path;
        private final List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;
        private final List<IdeToolEditionVersion> versions = new java.util.ArrayList<>();

        IdeToolEdition(String editionName, Path path) {
            this.editionName = editionName;
            this.path = path;
        }
    }
    
    /**
    * Class which represents individual versions of an edition of an IDE tool. Contains multiple parameter such as the name of the version, the path where it is installed,
    * a list of projects which use this version and a boolean whether the version is marked for deletion or not.
    */
    private static class IdeTool {
        String toolName;
        private final Path path;
        private final List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;
        private final List<IdeToolEdition> editions = new java.util.ArrayList<>();

        IdeTool(String toolName, Path path) {
            this.toolName = toolName;
            this.path = path;
        }
    }

    // List of installed IDE tools in the global software folder at $IDE_ROOT/_ide/software/default. This list is populated at the beginning of the cleanup process and then used to identify unused software and delete it.
    private final List<IdeTool> installedIdeTools = new java.util.ArrayList<>();

    public CleanupCommandlet(IdeContext context) {
    
        super(context);
        addKeyword(getName());
        this.forceDelete = add(new FlagProperty("--fd"));   // Force-Delete flag. Skips confirmation prompts if provided
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
        step.run(() -> {discoverAndDeleteUnusedSoftware();});

        LOG.debug("Finished cleanup commandlet");
    }

    /**
    * This method specified the primary flow for the discovery of installed and unused software, and its deletion.
    */
    private void discoverAndDeleteUnusedSoftware() {
        // Iterate over software in $IDE_ROOT/_ide/software folder and save installed software to a list
        discoverInstalledSoftware(this.context.getIdeRoot().resolve("_ide/software/default"));

        // Scan for IDEasy projects
        List<Path> ideasyProjects = this.context.getFileAccess().listChildren(this.context.getIdeRoot(), Files::isDirectory);

        // Iterate through IDEasy projects and scan software in software folder. Save found software to list
        for (Path ideasyProject : ideasyProjects) {
            if (ideasyProject.getFileName().toString().equals("_ide")) {
                continue;
            }
            discoverUsedSoftware(ideasyProject.resolve("software"), ideasyProject.getFileName().toString());
            discoverUsedSoftware(ideasyProject.resolve("software/extra"), ideasyProject.getFileName().toString());
        }

        // Mark unused software for deletion
        markUnusedSoftwareForDeletion();
        // Log summary report and proceed with deletion if user confirms
        logSoftwareToBeDeleted();
    }

    /**
    * This method discovers all installed tools at $IDE_ROOT/_ide/software/default and saves them to the list of installed tools.
    * Installed editions are then recusively discovered
    * @param software_folder The folder where the software is saved in ($IDE_ROOT/_ide/software/default)
    */
    private void discoverInstalledSoftware(Path software_folder) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeTool tool = new IdeTool(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            this.installedIdeTools.add(tool);
            discoverInstalledEditions(subfolder, tool);
        }
    }

    /**
    * This method discovers all installed editions of a tool at $IDE_ROOT/_ide/software/default/<tool> and saves them to the edition list of the tool.
    * Installed versions of the edition are then recusively discovered
    * @param edition_folder The folder where the editions are saved in ($IDE_ROOT/_ide/software/default/<tool>)
    * @param tool The respective tool for which we are discovering editions
    */
    private void discoverInstalledEditions(Path edition_folder, IdeTool tool) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(edition_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeToolEdition edition = new IdeToolEdition(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            tool.editions.add(edition);
            discoverInstalledVersions(subfolder, edition);
        }
    }

    /**
    * This method discovers all installed versions of an edition of a tool at $IDE_ROOT/_ide/software/default/<tool>/<edition> and saves them to the version list of the edition.
    * @param version_folder The folder where the versions are saved in ($IDE_ROOT/_ide/software/default/<tool>/<edition>)
    * @param edition The respective edition for which we are discovering versions
    */
    private void discoverInstalledVersions(Path version_folder, IdeToolEdition edition) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(version_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeToolEditionVersion version = new IdeToolEditionVersion(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            edition.versions.add(version);
        }
    }

    /**
    * This method scans the software folder of an IDEasy project for installed tools and matches these against the global tool list created earlier.
    * Identified tools are marked as used in the global tool list.
    * @param software_folder The software folder of the IDEasy project to scan for used software
    * @param project_name The name of the project we are currently scanning
    */
    private void discoverUsedSoftware(Path software_folder, String project_name) {
        // Get all installed tools for this project
        List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
        for (Path current_folder : subfolders) {
            // Converts the path of the tool installation to the real path by eliminating symlinks. This allows us to determine whether a tool is installed locally for an IDEasy project or is part
            // of the global software installation under $IDE_ROOT/_ide/software/default
            current_folder = this.context.getFileAccess().toRealPath(current_folder);
            // Check if directory contains ".ide.software.version" file. If so, read version and add to list of used software
            if (Files.exists(current_folder.resolve(".ide.software.version"))) {
                if (!current_folder.startsWith(this.context.getIdeRoot().resolve("_ide/software/default"))) {
                    // We found a software that is locally installed in an IDEasy project but not in the global software folder. We leave these alone.
                    return;
                }
                // Get details of the tool (name, edition, version)
                String tool_name = current_folder.getParent().getParent().getFileName().toString();
                String tool_edition = current_folder.getParent().getFileName().toString();
                String tool_version = current_folder.getFileName().toString();
                // Check if software exists in global IdeTool list. If so, mark as used
                for (IdeTool tool : this.installedIdeTools) {
                    if (tool.toolName.equals(tool_name)) {
                        tool.usedBy.add(project_name);
                        for (IdeToolEdition edition : tool.editions) {
                            if (edition.editionName.equals(tool_edition)) {
                                edition.usedBy.add(project_name);
                                for (IdeToolEditionVersion version : edition.versions) {
                                    if (version.versionName.equals(tool_version)) {
                                        version.usedBy.add(project_name);
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
    */
    private void markUnusedSoftwareForDeletion() {
        for (IdeTool tool : this.installedIdeTools) {
            for (IdeToolEdition edition : tool.editions) {
                for (IdeToolEditionVersion version : edition.versions) {
                    if (version.usedBy.isEmpty()) {
                        version.delete = true;
                    }
                }
                if (edition.usedBy.isEmpty()) {
                    edition.delete = true;
                }
            }
            if (tool.usedBy.isEmpty()) {
                tool.delete = true;
            }
        }
    }

    /**
    * Generates a summary report for tools, editions, and versions to be deleted and prompts the user for confirmation.
    * If the user agress, we proceed with deletion of the unused tools, editions, and version.
    */
    private void logSoftwareToBeDeleted() {
        String LogOutput = "";
        int totalToolsDeleted = 0;
        int totalEditionsDeleted = 0;
        int totalVersionsDeleted = 0;
        for (IdeTool tool : this.installedIdeTools) {
            String LogOutputEdition = "";
            int editionsDeleted = 0;
            for (IdeToolEdition edition : tool.editions) {
                String LogOutputVersion = "";
                int versionsDeleted = 0;
                for (IdeToolEditionVersion version : edition.versions) {
                    if (version.delete) {
                        LogOutputVersion += "\t\t - " + version.versionName + "\n";
                        versionsDeleted++;
                        totalVersionsDeleted++;
                    }
                }
                if (!LogOutputVersion.isBlank()) {
                    // If at least one version of the edition should be deleted
                    if (versionsDeleted < edition.versions.size()) {
                        LogOutputVersion += "\t\t + " + (edition.versions.size() - versionsDeleted) + " more version(s) of this edition will not be deleted\n";
                        LogOutputEdition += "\t - " + edition.editionName + "\n" + LogOutputVersion;
                    }
                    editionsDeleted++;
                    totalEditionsDeleted++;
                }
            }
            if (!LogOutputEdition.isBlank()) {
                // If at least one edition of the tool should have a delete operation
                if (editionsDeleted < tool.editions.size()) {
                    LogOutputEdition += "\t + " + (tool.editions.size() - editionsDeleted) + " more edition(s) of this tool will not be deleted\n";
                }
                LogOutput += " - " + tool.toolName + "\n" + LogOutputEdition;
                totalToolsDeleted++;
            }
        }

        if (LogOutput.isBlank()) {
            LOG.info("No installed tools will be deleted. All installed software is used by at least one project.");
        } else {
            LOG.info("The following installed tools will be deleted: \n" + LogOutput);
            LOG.info("Summary: {} installed tool versions across {} editions of {} tools will be deleted.", totalVersionsDeleted, totalEditionsDeleted, totalToolsDeleted);
            
            // Ask for conformation. Skipped if --fd flag is provided
            if (!this.forceDelete.isTrue()) {
                try {
                    this.context.askToContinue("Do you want to continue?");
                    
                } catch (CliAbortException e) {
                    LOG.info("Installed Tools will not be deleted.");
                    return;
                }
            }
            deleteUnusedSoftware();
        }
    }

    /**
    * Deletes tools, editions, and versions marked for deletion.
    */
    private void deleteUnusedSoftware() {
        int failed_deletion = 0;
        FileAccess fileAccess = this.context.getFileAccess();
        // Delete the tool
        for (IdeTool tool : this.installedIdeTools) {
            if (tool.delete) {
                LOG.debug("Deleting tool {} and all its editions and versions in {}", tool.toolName, tool.path);
                try {
                    fileAccess.delete(tool.path);
                } catch (Exception e) {
                    LOG.error("Failed to delete {}: {}", tool.path, e.getMessage());
                    failed_deletion++;
                }
                continue;
            }
            // Delete editions of the tool
            for (IdeToolEdition edition : tool.editions) {
                if (edition.delete) {
                    LOG.debug("Deleting edition {} of tool {} and all its versions in {}", edition.editionName, tool.toolName, edition.path);
                    try {
                        fileAccess.delete(edition.path);
                    } catch (Exception e) {
                        LOG.error("Failed to delete {}: {}", edition.path, e.getMessage());
                        failed_deletion++;
                    }
                    continue;
                }
                // Delete versions of the edition
                for (IdeToolEditionVersion version : edition.versions) {
                    if (version.delete) {
                        LOG.debug("Deleting version {} of edition {} of tool {} in {}", version.versionName, edition.editionName, tool.toolName, version.path);
                        try {
                            fileAccess.delete(version.path);
                        } catch (Exception e) {
                            LOG.error("Failed to delete {}: {}", version.path, e.getMessage());
                            failed_deletion++;
                        }
                    }
                }
            }
        }
        
        // Log completion message
        if (failed_deletion > 0) {
            LOG.warn("Unused tools have been deleted.\nFailed to delete {} files. Please check the log for details.", failed_deletion);
        } else {
            IdeLogLevel.SUCCESS.log(LOG, "Unused tools have been deleted successfully.");
        }
    }
}