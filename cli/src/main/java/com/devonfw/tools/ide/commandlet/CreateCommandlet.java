package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CreateCommandlet.class);

  /** {@link StringProperty} for the name of the new project */
  public final StringProperty newProject;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    this.newProject = add(new StringProperty("", true, "project"));
    add(this.settingsRepo);
  }

  @Override
  public String getName() {

    return "create";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  protected void doRun() {

    String newProjectName = this.newProject.getValue();
    Path newProjectPath = this.context.getIdeRoot().resolve(newProjectName);
    Path tempProjectPath = this.context.getIdeRoot().resolve("_ide/tmp/projects").resolve(newProjectName);

    LOG.info("Creating new IDEasy project in {}", newProjectPath);
    if (!this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory {} already exists. Do you want to continue?", newProjectPath);
    }

    initializeProject(tempProjectPath);
    this.context.setIdeHome(tempProjectPath);
    this.context.verifyIdeMinVersion(true);
    super.doRun();
    this.context.verifyIdeMinVersion(true);
    this.context.getFileAccess().writeFileContent(IdeVersion.getVersionString(), newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION));
    IdeLogLevel.SUCCESS.log(LOG, "Successfully created new project '{}'.", newProjectName);

    logWelcomeMessage();
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  @Override
  protected void updateSettings() {
    super.updateSettings();
    analyzeProject();
  }

    /**
   * This method is invoked when a new porject is created. It analyzes the cloned repository to check if it is a valid IDEasy repository.
   * The repository can either be a settings repository (with ide.properties or devon.properties on the top level)
   * or a code repository (with a settings folder on the top level containing such a file). Otherwise, the project creation fails and an error message is logged.
   */
  private void analyzeProject() {
    // Settings repository: ide.properties on top levels (or devon.properties for legacy users)
    // Code repository: settings folder on top level with ide.properties inside (or devon.properties for legacy users)
    String projectName = this.context.getProjectName();
    Path actualProjectPath = this.context.getIdeRoot().resolve(projectName);
    FileAccess fileAccess = this.context.getFileAccess();
    Path settingsPath = this.context.getSettingsPath();

    // Check whether the repository is a valid settings repository, code repository, or neither
    if (isSettingsRepository(settingsPath)) {
      LOG.info("The repository seems to be a settings repository based on the presence of " + EnvironmentVariables.DEFAULT_PROPERTIES + " or " + EnvironmentVariables.LEGACY_PROPERTIES + " on the top level.");
      moveProject(this.context.getIdeHome(), actualProjectPath);

    } else if (isCodeRepository(settingsPath)) {
      LOG.info(EnvironmentVariables.DEFAULT_PROPERTIES + " or " + EnvironmentVariables.LEGACY_PROPERTIES + " found in settings subfolder. This indicates a code repository with a settings folder on the top level.");
      
      String gitProjectName = GitUrl.of(this.settingsRepo.getValue(0)).getProjectName();
      Path codeFolderPath = actualProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN).resolve(gitProjectName);
      // Move temp project to actual project location $IDE_ROOT/<project_name>
      moveProject(this.context.getIdeHome(), actualProjectPath);

      // Move settings fodler containing code to $IDE_ROOT/<project_name>/workspaces/main/<git_project_name>
      moveProject(actualProjectPath.resolve(IdeContext.FOLDER_SETTINGS), codeFolderPath);

      // Set IDE_HOME to new (and actual) project location
      this.context.setIdeHome(actualProjectPath);

      // Link settings folder in IDE_HOME to settings folder in code repository
      fileAccess.symlink(codeFolderPath.resolve(IdeContext.FOLDER_SETTINGS), actualProjectPath.resolve(IdeContext.FOLDER_SETTINGS));

    } else {
      // Repository seems to be invalid. Clean up temporary location and return error
      fileAccess.delete(this.context.getIdeHome());
      throw new CliException("This repository does not include an " + EnvironmentVariables.DEFAULT_PROPERTIES + " or " + EnvironmentVariables.LEGACY_PROPERTIES + " file at the top level or a settings folder with such a file. "
      + "The repository does not seem to be a valid IDEasy repository. Please verify the repository and try again.");
    }
    // Set IDE_HOME to new (and actual) project location
    this.context.setIdeHome(actualProjectPath);
  }

  /**
   * Moves files of a new projectfrom the temporary location to the final project location.
   * @param oldPath - The path of the file or directory to be moved.
   * @param newPath - The path of the destination.
   */
  private void moveProject(Path oldPath, Path newPath) {
    FileAccess fileAccess = this.context.getFileAccess();
    try {
      fileAccess.mkdirs(newPath);
      fileAccess.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      LOG.error("Failed to move project from {} to {}. Please move it manually.", oldPath, newPath, e);
    }
  }

  /**
   * Checks whether te given repository is a settings repository by checking for the presence of ide.properties or devon.properties on the top level.
   * @param repositoryPath - The path of the repository to be checked.
   */
  private boolean isSettingsRepository(Path repositoryPath) {
    return Files.exists(repositoryPath.resolve(EnvironmentVariables.DEFAULT_PROPERTIES)) || Files.exists(repositoryPath.resolve(EnvironmentVariables.LEGACY_PROPERTIES));
  }

  /**
   * Checks whether te given repository is a code repository by checking for the presence of ide.properties or devon.properties within a settings folder on the top level.
   * @param repositoryPath - The path of the repository to be checked.
   */
  private boolean isCodeRepository(Path repositoryPath) {
    return isSettingsRepository(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS));
  }

  @Override
  protected String getStepMessage() {

    return "Create (Clone) repository";
  }

  private void logWelcomeMessage() {
    Path settingsFolder = this.context.getSettingsPath();
    if (Files.exists(settingsFolder)) {
      Predicate<Path> welcomePredicate = path -> String.valueOf(path.getFileName()).startsWith("welcome.");
      Path welcomeFilePath = this.context.getFileAccess().findFirst(settingsFolder, welcomePredicate, false);
      if (welcomeFilePath != null) {
        LOG.info(this.context.getFileAccess().readFileContent(welcomeFilePath));
      }
    }
  }
}
