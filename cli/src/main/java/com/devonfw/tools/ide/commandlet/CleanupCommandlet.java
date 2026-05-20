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

public class CleanupCommandlet extends Commandlet{

    public final FlagProperty forceDelete;

    private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

    private static class IdeToolEditionVersion {
        String versionName;
        private final Path path;
        private List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;

        IdeToolEditionVersion(String versionName, Path path) {
            this.versionName = versionName;
            this.path = path;
        }
    }
    
    private static class IdeToolEdition {
        String editionName;
        private final Path path;
        private List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;
        private List<IdeToolEditionVersion> versions = new java.util.ArrayList<>();

        IdeToolEdition(String editionName, Path path) {
            this.editionName = editionName;
            this.path = path;
        }
    }
    
    private static class IdeTool {
        String toolName;
        private final Path path;
        private List<String> usedBy = new java.util.ArrayList<>();
        private boolean delete = false;
        private List<IdeToolEdition> editions = new java.util.ArrayList<>();

        IdeTool(String toolName, Path path) {
            this.toolName = toolName;
            this.path = path;
        }
    }

    private final List<IdeTool> installedIdeTools = new java.util.ArrayList<>();

    public CleanupCommandlet(IdeContext context) {
    
        super(context);
        addKeyword(getName());
        this.forceDelete = add(new FlagProperty("--fd"));
    }

    
    @Override
    public String getName() {
    
        return "cleanup";
    }
    
    @Override
    protected void doRun() {

        LOG.debug("Start cleanup commandlet");

        Step step = context.newStep("Identify and remove unused software");
        step.run(() -> {discoverAndDeleteUnusedSoftware();});

        LOG.debug("Finished cleanup commandlet");
    }

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

        // Remove unused software
        markUnusedSoftwareForDeletion();
        logSoftwareToBeDeleted();
    }

    private void discoverInstalledSoftware(Path software_folder) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeTool tool = new IdeTool(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            this.installedIdeTools.add(tool);
            discoverInstalledEditions(subfolder, tool);
        }
    }

    private void discoverInstalledEditions(Path edition_folder, IdeTool tool) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(edition_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeToolEdition edition = new IdeToolEdition(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            tool.editions.add(edition);
            discoverInstalledVersions(subfolder, edition);
        }
    }

    private void discoverInstalledVersions(Path version_folder, IdeToolEdition edition) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(version_folder, Files::isDirectory);
        for (Path subfolder : subfolders) {
            IdeToolEditionVersion version = new IdeToolEditionVersion(subfolder.getFileName().toString(), this.context.getFileAccess().toRealPath(subfolder));
            edition.versions.add(version);
        }
    }

    private void discoverUsedSoftware(Path software_folder, String project_name) {
        List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
        for (Path current_folder : subfolders) {
            current_folder = this.context.getFileAccess().toRealPath(current_folder);
            // Check if directory contains ".ide.software.version" file. If so, read version and add to list of used software
            if (Files.exists(current_folder.resolve(".ide.software.version"))) {
                if (!current_folder.startsWith(this.context.getIdeRoot().resolve("_ide/software/default"))) {
                    // We found a software that is directly installed in an IDEasy project but not in the global software folder. We leave these alone
                    return;
                }
                String tool_name = current_folder.getParent().getParent().getFileName().toString();
                String tool_edition = current_folder.getParent().getFileName().toString();
                String tool_version = current_folder.getFileName().toString();
                // Check if software exists in IdeTool list. If so, mark as used
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
                    if (versionsDeleted < edition.versions.size()) {
                        LogOutputVersion += "\t\t + " + (edition.versions.size() - versionsDeleted) + " more version(s) of this edition will not be deleted\n";
                        LogOutputEdition += "\t - " + edition.editionName + "\n" + LogOutputVersion;
                    }
                    editionsDeleted++;
                    totalEditionsDeleted++;
                }
            }
            if (!LogOutputEdition.isBlank()) {
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

        if (failed_deletion > 0) {
            LOG.warn("Unused tools have been deleted.\nFailed to delete {} files. Please check the log for details.", failed_deletion);
        } else {
            IdeLogLevel.SUCCESS.log(LOG, "Unused tools have been deleted successfully.");
        }
    }
}