package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.RepositoryProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;

/**
 * {@link Commandlet} to setup one or multiple GIT repositories for development.
 */
public class RepositoryCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryCommandlet.class);

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
  protected void doRun() {

    Path repositoryFile = this.repository.getValue();

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      doImportRepository(repositoryFile, true);
    } else {
      // If no specific repository is provided, check for repositories folder
      Path repositoriesPath = this.context.getRepositoriesPath();
      if (repositoriesPath == null) {
        LOG.warn("Cannot find folder 'repositories' nor 'projects' in your settings.");
        return;
      }
      List<Path> propertiesFiles = this.context.getFileAccess()
          .listChildren(repositoriesPath, path -> path.getFileName().toString().endsWith(".properties"));
      boolean forceMode = this.context.isForceMode() || this.context.isForceRepositories();
      for (Path propertiesFile : propertiesFiles) {
        doImportRepository(propertiesFile, forceMode);
      }
    }
  }

  private void doImportRepository(Path repositoryFile, boolean forceMode) {

    String repositoryFilename = repositoryFile.getFileName().toString();
    final String repositoryId;
    if (repositoryFilename.endsWith(IdeContext.EXT_PROPERTIES)) {
      repositoryId = repositoryFilename.substring(0, repositoryFilename.length() - IdeContext.EXT_PROPERTIES.length());
    } else {
      repositoryId = repositoryFilename;
    }
    this.context.newStep("Setup of repository " + repositoryId, repositoryFile).run(() -> {
      doImportRepository(repositoryFile, forceMode, repositoryId);
    });
  }

  private void doImportRepository(Path repositoryFile, boolean forceMode, String repositoryId) {
    RepositoryConfig repositoryConfig = RepositoryConfig.loadProperties(repositoryFile, this.context);
    if (!repositoryConfig.active()) {
      if (forceMode) {
        LOG.info("Setup of repository {} is forced, hence proceeding ...", repositoryId);
      } else {
        LOG.info("Skipping repository {} because it is not active - use --force to setup all repositories ...", repositoryId);
        return;
      }
    }
    GitUrl gitUrl = repositoryConfig.asGitUrl();
    if (gitUrl == null) {
      // error was already logged.
      return;
    }
    LOG.debug("Repository configuration: {}", repositoryConfig);
    List<String> workspaces = repositoryConfig.workspaces();
    String repositoryRelativePath = repositoryConfig.path();
    if (repositoryRelativePath == null) {
      repositoryRelativePath = repositoryId;
    }
    Path ideStatusDir = this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE);
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(ideStatusDir);

    Path firstRepository = null;
    for (String workspaceName : workspaces) {
      Path workspacePath = this.context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve(workspaceName);
      Path repositoryPath = workspacePath.resolve(repositoryRelativePath);
      if (Files.isDirectory(repositoryPath.resolve(GitContext.GIT_FOLDER))) {
        if (firstRepository == null) {
          firstRepository = repositoryPath;
        }
        LOG.info("Repository {} already exists in workspace {} at {}", repositoryId, workspaceName, repositoryPath);
        if (!(this.context.isForceMode() || this.context.isForceRepositories())) {
          LOG.info("Ignoring repository {} in workspace {} - use --force or --force-repositories to rerun setup.", repositoryId, workspaceName);
          continue;
        }
      }
      Path repositoryCreatedStatusFile = ideStatusDir.resolve("repository." + repositoryId + "." + workspaceName);
      if (Files.exists(repositoryCreatedStatusFile)) {
        if (!(this.context.isForceMode() || this.context.isForceRepositories())) {
          LOG.info("Ignoring repository {} in workspace {} because it was already setup before - use --force or --force-repositories for recreation.",
              repositoryId, workspaceName);
          continue;
        }
      }
      if (firstRepository == null) {
        boolean success = cloneOrPullRepository(repositoryPath, gitUrl, repositoryCreatedStatusFile);
        if (success) {
          firstRepository = repositoryPath;
          buildRepository(repositoryConfig, repositoryPath);
          importRepository(repositoryConfig, repositoryPath, repositoryId);
        }
      } else {
        fileAccess.mkdirs(repositoryPath.getParent());
        fileAccess.symlink(firstRepository, repositoryPath);
      }
    }
  }

  private boolean cloneOrPullRepository(Path repositoryPath, GitUrl gitUrl, Path repositoryCreatedStatusFile) {

    FileAccess fileAccess = this.context.getFileAccess();
    return this.context.newStep("Clone or pull repository").run(() -> {
      fileAccess.mkdirs(repositoryPath);
      this.context.getGitContext().pullOrClone(gitUrl, repositoryPath);
      fileAccess.touch(repositoryCreatedStatusFile);
    });
  }

  private boolean buildRepository(RepositoryConfig repositoryConfig, Path repositoryPath) {
    String buildCmd = repositoryConfig.buildCmd();
    if (buildCmd != null && !buildCmd.isEmpty()) {
      return this.context.newStep("Build repository via: " + buildCmd).run(() -> {
        String[] command = buildCmd.split("\\s+");
        ToolCommandlet commandlet = this.context.getCommandletManager().getToolCommandlet(command[0]);
        if (commandlet == null) {
          String displayName = (command[0] == null || command[0].isBlank()) ? "<empty>" : "'" + command[0] + "'";
          LOG.error("Cannot build repository. Required tool '{}' not found. Please check your repository's build_cmd configuration value.",
              displayName);
          return;
        }
        commandlet.reset();
        for (int i = 1; i < command.length; i++) {
          commandlet.arguments.addValue(command[i]);
        }
        Path executionDirectory = repositoryPath;
        String path = repositoryConfig.buildPath();
        if (path != null) {
          executionDirectory = executionDirectory.resolve(path);
        }
        commandlet.setExecutionDirectory(executionDirectory);
        commandlet.run();
      });
    } else {
      LOG.debug("Build command not set. Skipping build for repository.");
      return true;
    }
  }

  private void importRepository(RepositoryConfig repositoryConfig, Path repositoryPath, String repositoryId) {

    Set<String> imports = repositoryConfig.imports();
    if ((imports == null) || imports.isEmpty()) {
      LOG.debug("Repository {} has no IDE configured for import.", repositoryId);
      return;
    }
    for (String ide : imports) {
      Step step = this.context.newStep("Importing repository " + repositoryId + " into " + ide);
      step.run(() -> {
        ToolCommandlet commandlet = this.context.getCommandletManager().getToolCommandlet(ide);
        if (commandlet == null) {
          String displayName = (ide == null || ide.isBlank()) ? "<empty>" : "'" + ide + "'";
          step.error("Cannot import repository '{}'. Required IDE '{}' not found. Please check your repository's imports configuration.", repositoryId,
              displayName);
        } else if (commandlet instanceof IdeToolCommandlet ideCommandlet) {
          ideCommandlet.importRepository(repositoryPath);
        } else {
          step.error("Repository {} has import {} configured that is not an IDE!", repositoryId, ide);
        }
      });
    }
  }
}
