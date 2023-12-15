package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PathProperty;
import com.devonfw.tools.ide.property.RepositoryProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.devonfw.tools.ide.commandlet.RepositoryConfig.loadProperties;

/**
 * {@link Commandlet} to setup a repository
 */
public class RepositoryCommandlet extends Commandlet {

  /** the repository to setup. */
  public final RepositoryProperty repository;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RepositoryCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    addKeyword("setup");
    this.repository = add(new RepositoryProperty("", false, "repository"));
  }

  @Override
  public String getName() {

    return "repository";
  }

  @Override
  public void run() {

    Path repositoriesPath = this.context.getSettingsPath().resolve(context.FOLDER_REPOSITORIES);
    Path legacyRepositoriesPath = this.context.getSettingsPath().resolve(context.FOLDER_LEGACY_REPOSITORIES);
    Path repositoryFile = repository.getValueAsPath(context);

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      doImportRepository(repositoryFile, true);
    } else {
      // If no specific repository is provided, check for repositories folder
      Path repositories;
      if (Files.exists(repositoriesPath)) {
        repositories = repositoriesPath;
      } else if (Files.exists(legacyRepositoriesPath)) {
        repositories = legacyRepositoriesPath;
      } else {
        this.context.warning("Cannot find repositories folder nor projects folder.");
        return;
      }

      List <Path> propertiesFiles = this.context.getFileAccess().getChildrenInDir(repositories,
          path -> path.getFileName().toString().endsWith(".properties"));

      boolean forceMode = this.context.isForceMode();
      for (Path propertiesFile : propertiesFiles) {
        doImportRepository(propertiesFile, forceMode);
      }
    }
  }

  private void doImportRepository(Path repositoryFile, boolean forceMode) {

    this.context.info("Importing repository from {} ...", repositoryFile.getFileName().toString());
    RepositoryConfig repositoryConfig = loadProperties(repositoryFile);

    if (!repositoryConfig.active()) {
      this.context.info("Repository is not active by default.");
      if (forceMode) {
        this.context.info("Repository setup is forced, hence proceeding ...");
      } else {
        this.context.info("Skipping repository - use force (-f) to setup all repositories ...");
        return;
      }
    }

    String repository = repositoryConfig.path();
    String gitUrl = repositoryConfig.gitUrl();
    if (repository == null || "".equals(repository) || gitUrl == null || "".equals(gitUrl)) {
      this.context.warning("Invalid repository configuration {} - both 'path' and 'git-url' have to be defined."
          , repositoryFile);
      return;
    }

    this.context.debug(repositoryConfig.toString());
    this.context.debug("Pull or clone git repository {} ...", repository);

    String workspace = repositoryConfig.workspace() != null ? repositoryConfig.workspace() : "main";
    Path workspacePath = this.context.getIdeHome().resolve("workspaces").resolve(workspace);
    this.context.getFileAccess().mkdirs(workspacePath);

    if (repositoryConfig.gitBranch() != null && !repositoryConfig.gitBranch().isEmpty()) {
      gitUrl = gitUrl + "#" + repositoryConfig.gitBranch();
    }

    Path repositoryPath = workspacePath.resolve(repository);
    this.context.gitPullOrClone(repositoryPath, gitUrl);

    String buildCmd = repositoryConfig.buildCmd();
    this.context.debug("Building repository with ide command: {}", buildCmd);
    if (buildCmd != null && !buildCmd.isEmpty()) {
      String[] command = buildCmd.split("\\s+");
      ToolCommandlet commandlet = this.context.getCommandletManager().getToolCommandlet(command[0]);
      List<String> args = new ArrayList<>(command.length - 1);
      for (int i = 1; i < command.length; i++) {
        args.add(command[i]);
      }
      commandlet.arguments.setValue(args);
      commandlet.run();
    } else {
      this.context.info("Build command not set. Skipping build for repository.");
    }

    if (!Files.exists(repositoryPath.resolve(".project"))) {
      for (String ideCommandlet : repositoryConfig.imports()) {
        //TODO: import repository to ideCommandlet
      }
    }
  }
}
