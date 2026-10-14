package com.devonfw.tools.ide.migration.v2026;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.migration.IdeVersionMigration;

/**
 * Migration to 2026.10.001. Moves the metadata of the JetBrains based IDEs ({@code .«ide»/config}) out of each workspace into the dedicated
 * {@code $IDE_HOME/.ide/«ide»/«workspace»/config} folder and removes the generated properties files (e.g. {@code idea.properties}) that IDEasy now generates
 * outside of the workspace. See <a href="https://github.com/devonfw/IDEasy/issues/2531">#2531</a>.
 */
public class Mig202610001 extends IdeVersionMigration {

  private static final Logger LOG = LoggerFactory.getLogger(Mig202610001.class);

  /** The name of the JetBrains IDEs mapped to the product prefix of their generated properties file. */
  private static final Map<String, String> JETBRAINS_IDES = Map.of("intellij", "idea", "android-studio", "studio", "pycharm", "pycharm");

  private static final String FOLDER_CONFIG = "config";

  /**
   * The constructor.
   */
  public Mig202610001() {

    super("2026.10.001");
  }

  @Override
  public void run(IdeContext context) {

    Path workspacesPath = context.getWorkspacesBasePath();
    if (workspacesPath == null) {
      return;
    }
    FileAccess fileAccess = context.getFileAccess();
    Path dotIde = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE);
    List<Path> workspaces = fileAccess.listChildren(workspacesPath, Files::isDirectory);
    for (Path workspace : workspaces) {
      String workspaceName = workspace.getFileName().toString();
      for (Entry<String, String> ide : JETBRAINS_IDES.entrySet()) {
        migrateWorkspace(fileAccess, workspace, dotIde.resolve(ide.getKey()).resolve(workspaceName), "." + ide.getKey(), ide.getValue() + ".properties");
      }
    }
  }

  private void migrateWorkspace(FileAccess fileAccess, Path workspace, Path ideMetadata, String legacyMetadataName, String propertiesName) {

    Path legacyMetadata = workspace.resolve(legacyMetadataName);
    if (Files.isDirectory(legacyMetadata)) {
      Path legacyConfig = legacyMetadata.resolve(FOLDER_CONFIG);
      Path config = ideMetadata.resolve(FOLDER_CONFIG);
      if (Files.isDirectory(legacyConfig)) {
        if (Files.exists(config)) {
          LOG.warn("Skipping migration of {} since target already exists: {}", legacyConfig, config);
        } else {
          fileAccess.mkdirs(config.getParent());
          fileAccess.move(legacyConfig, config);
        }
      }
      // remove the legacy folder if nothing is left in it so the workspace stays clean
      if (fileAccess.isEmptyDir(legacyMetadata)) {
        fileAccess.delete(legacyMetadata);
      }
    }
    Path legacyProperties = workspace.resolve(propertiesName);
    if (Files.exists(legacyProperties)) {
      LOG.info("Removing obsolete file {} since it is now generated in {}.", legacyProperties, ideMetadata);
      fileAccess.backup(legacyProperties);
    }
  }

}
