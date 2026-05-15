package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

public class CleanupCommandlet extends Commandlet{

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

        // Scan for IDEasy projects
        List<Path> ideasyProjects = this.context.getFileAccess().listChildren(this.context.getIdeRoot(), Files::isDirectory);

        // Iterate through IDEasy projects and scan software in software folder. Save found software to list
        for (Path ideasyProject : ideasyProjects) {
            scan_software_folder(ideasyProject.resolve("software"));
        }


        // Iterate over software in $IDE_ROOT/_ide/software folder and save unused software to a list

        // Remove unused software


    }

    private void scan_software_folder(Path software_folder) {
        
        // Check if directory contains "ide.software.version" file. If so, read version and add to list of used software
        if (Files.exists(software_folder.resolve("ide.software.version"))) {
            // read version and add to list of used software
            return;
        }

        // If not, get all subfolders and recursively iterate over them
        try {
            List<Path> subfolders = this.context.getFileAccess().listChildren(software_folder, Files::isDirectory);
            for (Path subfolder : subfolders) {
                scan_software_folder(subfolder);
            }
        } catch (Exception e) {
            // log error and return
            return;
        }

    }

}
