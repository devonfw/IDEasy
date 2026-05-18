package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

public class CleanupCommandlet extends Commandlet{

    private static final Logger LOG = LoggerFactory.getLogger(CleanupCommandlet.class);

    private static class IdeTool {
        String tool_name;
        String tool_edition;
        String tool_version;
        private final Path path;
        private boolean used;

        IdeTool(String tool_name, String tool_edition, String tool_version, Path path) {
            this.tool_name = tool_name;
            this.tool_edition = tool_edition;
            this.tool_version = tool_version;
            this.path = path;
            this.used = false;
        }
    }

    private final List<IdeTool> installedIdeTools = new java.util.ArrayList<>();

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
        // Iterate over software in $IDE_ROOT/_ide/software folder and save installed software to a list
        scanSoftwareFolder(this.context.getIdeRoot().resolve("_ide").resolve("software"), true);

        // Scan for IDEasy projects
        List<Path> ideasyProjects = this.context.getFileAccess().listChildren(this.context.getIdeRoot(), Files::isDirectory);

        // Iterate through IDEasy projects and scan software in software folder. Save found software to list
        for (Path ideasyProject : ideasyProjects) {
            if (ideasyProject.getFileName().toString().equals("_ide")) {
                continue;
            }
            scanSoftwareFolder(ideasyProject.resolve("software"), false);
        }



        // Remove unused software


    }

    private void scanSoftwareFolder(Path software_folder, Boolean discover) {
        
        software_folder = this.context.getFileAccess().toRealPath(software_folder);
        LOG.debug("Scanning software folder: " +software_folder);  // DEBUG
        // Check if directory contains ".ide.software.version" file. If so, read version and add to list of used software
        if (Files.exists(software_folder.resolve(".ide.software.version"))) {
            LOG.debug("Found software folder: " + software_folder);  // DEBUG
            if (discover) {
                String tool_name = software_folder.getParent().getParent().getFileName().toString();
                String tool_edition = software_folder.getParent().getFileName().toString();
                String tool_version = software_folder.getFileName().toString();
                LOG.debug("Discovered software at path: " + software_folder);  // DEBUG
                this.installedIdeTools.add(new IdeTool(tool_name, tool_edition, tool_version, software_folder));
            } else {
                // Check if software with name and version exists in IdeTool list. If so, mark as used
                for (IdeTool tool : this.installedIdeTools) {
                    if (tool.path.toString().equals(software_folder.toString())) {
                        tool.used = true;
                        LOG.debug("Marked software as used: " + software_folder);  // DEBUG
                        break;
                    }
                }
            }
            return;
        }

        // If not, get all subfolders and recursively iterate over them
        try {
            List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
            for (Path subfolder : subfolders) {
                scanSoftwareFolder(subfolder, discover);
            }
        } catch (Exception e) {
            // log error and return
            return;
        }

    }

}
