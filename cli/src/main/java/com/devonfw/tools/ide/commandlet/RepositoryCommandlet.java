package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.RepositoryProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.devonfw.tools.ide.commandlet.RepositoryConfig.loadProperties;

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

    Path repositoryFile = repository.getValue();

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      doImportRepository(repositoryFile, true);
    } else {
      // If no specific repository is provided, check for repositories folder
      Path repositoriesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
      Path legacyRepositoriesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_REPOSITORIES);
      Path repositories;
      if (Files.exists(repositoriesPath)) {
        repositories = repositoriesPath;
      } else if (Files.exists(legacyRepositoriesPath)) {
        repositories = legacyRepositoriesPath;
      } else {
        this.context.warning("Cannot find repositories folder nor projects folder.");
        return;
      }

      List<Path> propertiesFiles = this.context.getFileAccess().listChildren(repositories, path -> path.getFileName().toString().endsWith(".properties"));

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
      this.context.warning("Invalid repository configuration {} - both 'path' and 'git-url' have to be defined.", repositoryFile.getFileName().toString());
      return;
    }

    this.context.debug(repositoryConfig.toString());

    String workspace = repositoryConfig.workspace() != null ? repositoryConfig.workspace() : "main";
    Path workspacePath = this.context.getIdeHome().resolve("workspaces").resolve(workspace);
    this.context.getFileAccess().mkdirs(workspacePath);

    Path repositoryPath = workspacePath.resolve(repository);
    this.context.getGitContext().pullOrClone(gitUrl, repositoryConfig.gitBranch(), repositoryPath);

    String buildCmd = repositoryConfig.buildCmd();
    this.context.debug("Building repository with ide command: {}", buildCmd);
    if (buildCmd != null && !buildCmd.isEmpty()) {
      String[] command = buildCmd.split("\\s+");
      ToolCommandlet commandlet = this.context.getCommandletManager().getToolCommandlet(command[0]);

      for (int i = 1; i < command.length; i++) {
        commandlet.arguments.addValue(command[i]);
      }
      commandlet.run();
    } else {
      this.context.info("Build command not set. Skipping build for repository.");
    }

  }
}
