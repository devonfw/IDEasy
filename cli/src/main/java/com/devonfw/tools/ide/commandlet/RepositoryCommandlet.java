package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.property.RepositoryProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link Commandlet} to setup one or multiple GIT repositories for development.
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

    Path repositoryFile = this.repository.getValue();

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      doImportRepository(repositoryFile, true);
    } else {
      // If no specific repository is provided, check for repositories folder
      Path repositoriesPath = this.context.getRepositoriesPath();
      if (repositoriesPath == null) {
        this.context.warning("Cannot find folder 'repositories' nor 'projects' in your settings.");
        return;
      }
      List<Path> propertiesFiles = this.context.getFileAccess()
          .listChildren(repositoriesPath, path -> path.getFileName().toString().endsWith(".properties"));
      boolean forceMode = this.context.isForceMode();
      for (Path propertiesFile : propertiesFiles) {
        doImportRepository(propertiesFile, forceMode);
      }
    }
  }

  private void doImportRepository(Path repositoryFile, boolean forceMode) {

    this.context.info("Importing repository from {} ...", repositoryFile.getFileName().toString());
    RepositoryConfig repositoryConfig = RepositoryConfig.loadProperties(repositoryFile);

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
    if (repository == null || repository.isEmpty() || gitUrl == null || gitUrl.isEmpty()) {
      this.context.warning("Invalid repository configuration {} - both 'path' and 'git-url' have to be defined.", repositoryFile.getFileName().toString());
      return;
    }

    this.context.debug(repositoryConfig.toString());

    String workspace = repositoryConfig.workspace();
    if (workspace == null) {
      workspace = IdeContext.WORKSPACE_MAIN;
    }
    Path workspacePath = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspace);
    this.context.getFileAccess().mkdirs(workspacePath);

    Path repositoryPath = workspacePath.resolve(repository);
    if (!Files.isDirectory(repositoryPath.resolve(GitContext.GIT_FOLDER))) {
      this.context.getGitContext().pullOrClone(repositoryConfig.asGitUrl(), repositoryPath);
    }

    String buildCmd = repositoryConfig.buildCmd();
    this.context.debug("Building repository with ide command: {}", buildCmd);
    if (buildCmd != null && !buildCmd.isEmpty()) {
      String[] command = buildCmd.split("\\s+");
      ToolCommandlet commandlet = this.context.getCommandletManager().getRequiredToolCommandlet(command[0]);

      for (int i = 1; i < command.length; i++) {
        commandlet.arguments.addValue(command[i]);
      }
      commandlet.run();
    } else {
      this.context.info("Build command not set. Skipping build for repository.");
    }

  }
}
