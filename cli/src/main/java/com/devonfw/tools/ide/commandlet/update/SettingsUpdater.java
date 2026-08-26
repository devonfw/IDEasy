package com.devonfw.tools.ide.commandlet.update;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliRethrowException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.git.repository.RepositoryType;
import com.devonfw.tools.ide.git.repository.RepositoryUtil;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * Handles updating/cloning of the settings repository. Returns a result indicating the outcome of the settings update operation.
 */
public class SettingsUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SettingsUpdater.class);

  private final AbstractIdeContext context;
  private final StringProperty settingsRepoProperty;

  private static final String MESSAGE_SETTINGS_REPO_URL = """
      No settings found at {} and no SETTINGS_URL is defined.
      Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
      Please contact the technical lead of your project to get the SETTINGS_URL for your project to enter.
      In case you just want to test IDEasy you may simply hit return to install the default settings.""";

  /**
   * Result of the settings update operation.
   */
  public enum ResultStatus {
    /** Settings repository was updated via pull and is valid. */
    SETTINGS_UPDATED,
    /** Settings repository was cloned from scratch (blank state). */
    SETTINGS_CLONED,
    /** Settings update failed (could not clone or invalid repository). */
    SETTINGS_UPDATE_FAILED
  }

  /**
   * Result object containing the outcome and repository type.
   */
  public record SettingsUpdateResult(ResultStatus status, RepositoryType repositoryType) {

  }

  /**
   * Creates a new SettingsUpdater.
   *
   * @param context the IDE context
   * @param settingsRepoProperty the settings repository property from the update commandlet
   */
  public SettingsUpdater(AbstractIdeContext context, StringProperty settingsRepoProperty) {
    this.context = context;
    this.settingsRepoProperty = settingsRepoProperty;
  }

  /**
   * Updates the settings repository by either pulling (if exists) or cloning (if new).
   *
   * @param codeRepository whether this is a code repository (skip pull if true and not forced)
   * @return the result of the settings update operation
   */
  public SettingsUpdateResult updateSettings(boolean codeRepository) {

    Path settingsPath = this.context.getSettingsPath();
    boolean isSettingsRepo = this.context.getGitContext().isGitRepo(settingsPath);

    // If it's a code repository and not forced, skip the pull
    if (codeRepository && isSettingsRepo && !this.context.isForceMode()) {
      LOG.info("Skipping git pull in settings due to code repository. Use --force-pull to enforce pulling.");
      return new SettingsUpdateResult(ResultStatus.SETTINGS_UPDATED, RepositoryType.SETTINGS);
    }

    if (isSettingsRepo) {
      // Existing settings repository - pull updates
      return pullExistingSettings(settingsPath);
    } else {
      // No existing settings - clone from scratch
      return cloneSettings();
    }
  }

  private SettingsUpdateResult pullExistingSettings(Path settingsPath) {

    GitContext gitContext = this.context.getGitContext();
    if (gitContext.hasUntrackedFiles(settingsPath)) {
      gitContext.pullSafelyWithStash(settingsPath);
    } else {
      gitContext.pull(settingsPath);
    }
    this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
    return new SettingsUpdateResult(ResultStatus.SETTINGS_UPDATED, RepositoryType.SETTINGS);
  }

  private SettingsUpdateResult cloneSettings() {

    Path tempProjectPath = null;
    try {
      // Get settings URL
      GitUrl gitUrl = getOrAskSettingsUrl();

      // Use unique temp directory to avoid leftovers from previous attempts
      tempProjectPath = createUniqueTempProjectPath();
      this.context.getGitContext().pullOrClone(gitUrl, tempProjectPath);
      return checkIntegrityAndMove(tempProjectPath, gitUrl.getProjectName());
    } catch (Exception e) {
      // Clean up temp directory on failure
      if (tempProjectPath != null) {
        this.context.getFileAccess().backup(tempProjectPath);
      }
      throw new CliRethrowException("Settings repository integrity check failed: " + e.getMessage(), e);
    }
  }

  private GitUrl getOrAskSettingsUrl() {

    String repository = this.settingsRepoProperty.getValue();
    repository = handleDefaultRepository(repository);
    String userPrompt = "Settings URL [" + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
    String defaultUrl = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    LOG.info(MESSAGE_SETTINGS_REPO_URL, this.context.getSettingsPath());

    GitUrl gitUrl = null;
    if (repository != null) {
      gitUrl = GitUrl.of(repository);
    }
    while ((gitUrl == null) || !gitUrl.isValid()) {
      repository = this.context.askForInput(userPrompt, defaultUrl);
      repository = handleDefaultRepository(repository);
      gitUrl = GitUrl.of(repository);
      if (!gitUrl.isValid()) {
        LOG.warn("The input URL is not valid, please try again.");
      }
    }
    return gitUrl;
  }

  private Path createUniqueTempProjectPath() {

    // Use FileAccess.createTempDir to ensure unique directory and avoid leftovers
    FileAccess fileAccess = this.context.getFileAccess();
    Path tempProjectsDir = this.context.getTempPath().resolve(IdeContext.FOLDER_PROJECTS);
    fileAccess.mkdirs(tempProjectsDir);
    return fileAccess.createTempDir(this.context.getProjectName() + "-");
  }

  private SettingsUpdateResult checkIntegrityAndMove(Path projectPath, String gitProjectName) {

    FileAccess fileAccess = this.context.getFileAccess();

    if (!Files.exists(projectPath)) {
      throw new CliRethrowException("Git pull target folder does not exist.");
    }

    Path targetDirectory;
    RepositoryType repoType = RepositoryUtil.getRepositoryType(projectPath, gitProjectName);

    switch (repoType) {
      case SETTINGS -> {
        targetDirectory = this.context.getIdeHome().resolve(IdeContext.FOLDER_SETTINGS);
        moveProject(projectPath, targetDirectory);
        this.context.getGitContext().saveCurrentCommitId(targetDirectory, this.context.getSettingsCommitIdPath());
        return new SettingsUpdateResult(ResultStatus.SETTINGS_CLONED, RepositoryType.SETTINGS);
      }
      case CODE_SETTINGS_COMBINED -> {
        // Special case: symlink from IDE_HOME/settings to workspace/repo_name/settings
        targetDirectory = this.context.getWorkspacePath().resolve(gitProjectName);
        moveProject(projectPath, targetDirectory);

        Path symlinkPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_SETTINGS);
        Path symlinkTargetPath = this.context.getWorkspacePath().resolve(gitProjectName).resolve(IdeContext.FOLDER_SETTINGS);

        fileAccess.symlink(symlinkTargetPath, symlinkPath);
        this.context.getGitContext().saveCurrentCommitId(symlinkTargetPath, this.context.getSettingsCommitIdPath());
        return new SettingsUpdateResult(ResultStatus.SETTINGS_CLONED, RepositoryType.CODE_SETTINGS_COMBINED);
      }
      default -> {
        fileAccess.backup(projectPath);
        throw new CliRethrowException(getIntegrityCheckErrorMessage(String.format(
            "The given git repository URL does not point to a valid settings or code-settings repository. "
                + "Please verify and try again. Before trying again, please delete the folder %s",
            this.context.getIdeHome())));
      }
    }
  }

  private Path moveProject(Path from, Path to) {

    FileAccess fileAccess = this.context.getFileAccess();
    try {
      fileAccess.move(from, to);
    } catch (Exception e) {
      throw new CliRethrowException(String.format("Failed to move project from %s to %s", from, to), e);
    }
    return to;
  }

  private String getIntegrityCheckErrorMessage(String message) {
    return String.format("Settings repository integrity check failed: %s", message);
  }

  private String handleDefaultRepository(String repository) {
    if ("-".equals(repository)) {
      LOG.info("'-' was found for the repository, the default settings repository '{}' will be used.", IdeContext.DEFAULT_SETTINGS_REPO_URL);
      repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    }
    return repository;
  }
}
