package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PathProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.util.FilenameUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class RepositoryCommandlet extends Commandlet {


  private PathProperty repository;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RepositoryCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    add(new StringProperty("setup", true, null));
    this.repository = add(new PathProperty("", false, "repository"));
  }

  @Override
  public String getName() {

    return "repository";
  }

  @Override
  public void run() {

    Path repositoriesPath = this.context.getSettingsPath().resolve("repositories");
    Path legacyRepositoriesPath = this.context.getSettingsPath().resolve("projects");
    Path repositoryFile = repository.getValue();

    if (repositoryFile != null) {
      // Handle the case when a specific repository is provided
      if (!Files.exists(repositoryFile)) {
        repositoryFile = repositoriesPath.resolve(repositoryFile.toString() + ".properties");
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
      Path repositories = Files.exists(repositoriesPath) ? repositoriesPath :
          Files.exists(legacyRepositoriesPath) ? legacyRepositoriesPath : null;

      if (repositories == null) return;

      List <Path> propertiesFiles = this.context.getFileAccess().getFilesInDir(repositories,
          path -> "properties".equals(FilenameUtil.getExtension(path.getFileName().toString())));

      if (propertiesFiles != null) {
        boolean forceMode = this.context.isForceMode();
        for (Path propertiesFile : propertiesFiles) {
          doImportRepository(propertiesFile, forceMode);
        }
      }
    }
  }

  private void doImportRepository(Path repositoryFile, boolean forceMode) {

    this.context.info("Importing from {} ...", repositoryFile.toString());
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
    if (repository == null || ( repository != null && repository.isEmpty()) || gitUrl == null || (gitUrl != null && gitUrl.isEmpty())) {
      this.context.warning("Invalid repository configuration {} - both 'path' and 'git-url' have to be defined."
          , repositoryFile);
      return;
    }

    this.context.debug(repositoryConfig.toString());
    this.context.debug("Pull or clone git repository {} ...", repository);

    String workspace = repositoryConfig.workspace() != null ? repositoryConfig.workspace() : "main";
    Path repositoryWorkspacePath = this.context.getIdeHome().resolve("workspaces").resolve(workspace);
    this.context.getFileAccess().mkdirs(repositoryWorkspacePath);

    String targetGitUrl = repositoryConfig.gitUrl();
    if (repositoryConfig.gitBranch() != null && !repositoryConfig.gitBranch().isEmpty()) {
      targetGitUrl = targetGitUrl + "#" + repositoryConfig.gitBranch();
    }

    Path repositoryPath = repositoryWorkspacePath.resolve(repository);
    this.context.gitPullOrClone(repositoryPath, targetGitUrl);

    String buildCmd = repositoryConfig.buildCmd();
    this.context.debug("Building project with ide command: {}", buildCmd);
    if (buildCmd != null && !buildCmd.isEmpty()) {
      //TODO: build repository build path
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
        properties.getProperty("build_cmd"),
        properties.getProperty("eclipse"),
        Boolean.parseBoolean(properties.getProperty("active"))
    );
  }

}
