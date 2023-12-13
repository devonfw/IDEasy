package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.property.PathProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.util.FilenameUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * {@link Commandlet} to setup a repository
 */
public class RepositoryCommandlet extends Commandlet {

  /** the repository to setup. */
  public final PathProperty repository;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RepositoryCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    addKeyword("setup");
    this.repository = add(new PathProperty("", false, "repository"));
  }

  @Override
  public String getName() {

    return "repository";
  }

  @Override
  public void run() {

    Path repositoriesPath = this.context.getSettingsPath().resolve(context.FOLDER_REPOSITORIES);
    Path legacyRepositoriesPath = this.context.getSettingsPath().resolve(context.FOLDER_LEGACY_REPOSITORIES);
    Path repositoryFile = repository.getValue();

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      if (!Files.exists(repositoryFile)) {
        repositoryFile = repositoriesPath.resolve(repositoryFile.getFileName().toString() + ".properties");
      }
      if (!Files.exists(repositoryFile)) {
        Path legacyRepositoryFile = legacyRepositoriesPath.resolve(repositoryFile.getFileName().toString());
        if (Files.exists(legacyRepositoryFile)) {
          repositoryFile = legacyRepositoryFile;
        } else {
          this.context.warning("Could not find " + repositoryFile);
          return;
        }
      }
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

      List <Path> propertiesFiles = this.context.getFileAccess().getFilesInDir(repositories,
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
    this.context.debug("Building project with ide command: {}", buildCmd);
    if (buildCmd != null && !buildCmd.isEmpty()) {
      String[] command = buildCmd.split("\\s+");
      this.context.getCommandletManager().getToolCommandlet(command[0]).install(true);
      ProcessContext pc = this.context.newProcess();
      pc.executable(command[0]);
      pc.addArgs(Arrays.copyOfRange(command, 1, command.length));
      Path buildPath = repositoryPath;
      if (repositoryConfig.buildPath() != null) {
        buildPath = buildPath.resolve(repositoryConfig.buildPath());
      }
      pc.directory(buildPath);
      pc.run();
    } else {
      this.context.info("Build command not set. Skipping build for repository.");
    }

    if ("import".equals(repositoryConfig.eclipse()) && !Files.exists(repositoryPath.resolve(".project"))) {
      //TODO: import repository to eclipse
    }
  }


  private RepositoryConfig loadProperties(Path filePath) {

    Properties properties = new Properties();
    try (InputStream input = new FileInputStream(filePath.toString())) {
      properties.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + filePath, e);
    }

    return new RepositoryConfig(
        properties.getProperty("path"),
        properties.getProperty("workingsets"),
        properties.getProperty("workspace"),
        properties.getProperty("git_url"),
        properties.getProperty("git_branch"),
        properties.getProperty(("build_path")),
        properties.getProperty("build_cmd"),
        properties.getProperty("eclipse"),
        Boolean.parseBoolean(properties.getProperty("active"))
    );
  }

}
