package com.devonfw.tools.ide.migration.v2026;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.migration.IdeVersionMigration;

/**
 * Migration to 2026.09.002. Moves the VSCode user-data folder ({@code .vscode/.userdata}) out of each workspace into the dedicated
 * {@code $IDE_HOME/.ide/vscode/«workspace»/config} folder so workspaces stay clean and independent of the IDE being used. See
 * <a href="https://github.com/devonfw/IDEasy/issues/2142">#2142</a>.
 */
public class Mig202609002 extends IdeVersionMigration {

  private static final Logger LOG = LoggerFactory.getLogger(Mig202609002.class);

  /**
   * The constructor.
   */
  public Mig202609002() {

    super("2026.09.002");
  }

  @Override
  public void run(IdeContext context) {

    Path workspacesPath = context.getWorkspacesBasePath();
    if (workspacesPath == null) {
      return;
    }
    FileAccess fileAccess = context.getFileAccess();
    Path vscodeMetaPath = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("vscode");
    List<Path> workspaces = fileAccess.listChildren(workspacesPath, Files::isDirectory);
    for (Path workspace : workspaces) {
      Path oldUserData = workspace.resolve(".vscode").resolve(".userdata");
      if (fileAccess.isExpectedFolder(oldUserData)) {
        Path target = vscodeMetaPath.resolve(workspace.getFileName().toString()).resolve("config");
        if (Files.exists(target)) {
          LOG.warn("Skipping migration of {} since target already exists: {}", oldUserData, target);
          continue;
        }
        fileAccess.mkdirs(target.getParent());
        fileAccess.move(oldUserData, target);
      }
    }
  }

}
