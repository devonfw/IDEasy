package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
      RepositoryConfig config = prepareActiveRepository(repositoryFile, true);
      if (config != null) {
        importRepository(config);
      }
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
      Map<Path, RepositoryConfig> repositoryConfigMap = new HashMap<>(propertiesFiles.size());
      for (Path propertiesFile : propertiesFiles) {
        RepositoryConfig config = prepareActiveRepository(propertiesFile, forceMode);
        if (config != null) {
          repositoryConfigMap.put(propertiesFile, config);
        }
      }
      for (Path propertiesFile : propertiesFiles) {
        RepositoryConfig config = repositoryConfigMap.get(propertiesFile);
        if (config != null) {
          importRepository(config);
        }
      }
    }
  }

  private RepositoryConfig prepareActiveRepository(Path repositoryFile, boolean forceMode) {

    RepositoryConfig config = RepositoryConfig.loadProperties(repositoryFile, this.context);
    if (config == null) {
      return null;
    }
    if (!config.active()) {
      if (forceMode) {
        LOG.info("Setup of repository {} is forced, hence proceeding ...", config.id());
      } else {
        LOG.info("Skipping repository {} because it is not active, use --force-repositories to setup all repositories ...", config.id());
        return null;
      }
    }
    // prepare workspace creation for correct resolution of *
    List<String> workspaces = config.workspaces();
    for (String workspace : workspaces) {
      if (!RepositoryConfig.WORKSPACE_NAME_ALL.equals(workspace)) {
        Path workspacePath = this.context.getWorkspacePath(workspace);
        this.context.getFileAccess().mkdirs(workspacePath);
      }
    }
    return config;
  }

  private void importRepository(RepositoryConfig config) {

    this.context.newStep("Setup of repository " + config.id()).run(() -> {
      doImportRepository(config);
    });
  }

  private void doImportRepository(RepositoryConfig config) {
    LOG.debug("Repository configuration: {}", config);
    String repositoryRelativePath = config.path();
    if (repositoryRelativePath == null) {
      repositoryRelativePath = config.id();
    }
    Path ideStatusDir = this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE);
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(ideStatusDir);

    List<String> workspaces = config.workspaces();
    if ((workspaces.size() == 1) && (RepositoryConfig.WORKSPACE_NAME_ALL.equals(workspaces.getFirst()))) {
      // if workspaces=* replace with all existing workspaces
      workspaces = fileAccess.listChildren(this.context.getWorkspacesBasePath(), Files::isDirectory).stream().map(Path::getFileName).map(Path::toString)
          .toList();
    }
    // if main is contained in workspaces, it should come first (to ensure physical cloning to main and linking to others)
    if (!workspaces.getFirst().equals(IdeContext.WORKSPACE_MAIN) && workspaces.contains(IdeContext.WORKSPACE_MAIN)) {
      workspaces = new ArrayList<>(workspaces); // mutable copy
      workspaces.remove(IdeContext.WORKSPACE_MAIN);
      workspaces.addFirst(IdeContext.WORKSPACE_MAIN);
    }
    Path firstRepository = null;
    for (String workspaceName : workspaces) {
      Path workspacePath = this.context.getWorkspacePath(workspaceName);
      Path repositoryPath = workspacePath.resolve(repositoryRelativePath);
      Path repositoryCreatedStatusFile = ideStatusDir.resolve("repository." + config.id() + "." + workspaceName);
      boolean createRepository = true;
      if (Files.isDirectory(repositoryPath.resolve(GitContext.GIT_FOLDER))) {
        if (firstRepository == null) {
          firstRepository = repositoryPath;
        }
        LOG.info("Repository {} already exists in workspace {} at {}", config.id(), workspaceName, repositoryPath);
        if (!(this.context.isForceMode() || this.context.isForceRepositories())) {
          LOG.info("Ignoring repository {} in workspace {}, use --force-repositories to rerun setup.", config.id(), workspaceName);
          createRepository = false;
        }
      }
      if (Files.exists(repositoryCreatedStatusFile)) {
        if (!(this.context.isForceMode() || this.context.isForceRepositories())) {
          LOG.info("Ignoring repository {} in workspace {} because it was already setup before, use --force-repositories for recreation.",
              config.id(), workspaceName);
          createRepository = false;
        }
      }
      if (createRepository) {
        if (firstRepository == null) {
          GitUrl gitUrl = config.asGitUrl();
          boolean success = cloneOrPullRepository(repositoryPath, gitUrl, repositoryCreatedStatusFile);
          if (success) {
            firstRepository = repositoryPath;
            buildRepository(config, repositoryPath);
            importRepository(config, repositoryPath, config.id());
          }
        } else {
          fileAccess.mkdirs(repositoryPath.getParent());
          fileAccess.symlink(firstRepository, repositoryPath);
        }
      }
      if (Files.exists(repositoryPath)) {
        for (RepositoryLink link : config.links()) {
          createRepositoryLink(link, repositoryPath, workspacePath);
        }
      }
    }
  }

  private void createRepositoryLink(RepositoryLink link, Path repositoryPath, Path workspacePath) {
    Path linkPath = workspacePath.resolve(link.link());
    String target = link.target();
    Path linkTargetPath;
    if ((target != null) && !target.isBlank()) {
      linkTargetPath = repositoryPath.resolve(target);
      if (!Files.exists(linkTargetPath)) {
        LOG.error("Skipping link from '{}' to '{}' because target does not exist: {}", link.link(), target, linkTargetPath);
        return;
      }
    } else {
      linkTargetPath = repositoryPath;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(linkPath.getParent());
    fileAccess.symlink(linkTargetPath, linkPath);
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
