package com.devonfw.tools.ide.commandlet.update;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliRethrowException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.git.repository.RepositoryType;
import com.devonfw.tools.ide.git.repository.RepositoryUtil;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * Handles the settings repository of the current project in two phases:
 * <ol>
 * <li>{@link #checkSettings() health check}: the settings are always cloned into a temporary directory first where it is verified that the git URL is valid,
 * that cloning succeeded, and that the repository actually is a settings or a combined code and settings repository.</li>
 * <li>{@link #applySettings(SettingsUpdateResult) apply}: only after the health check succeeded the settings are either pulled in place (if they were already
 * present) or the verified clone is moved to its final location.</li>
 * </ol>
 */
public class SettingsUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SettingsUpdater.class);

  private static final String MESSAGE_SETTINGS_REPO_URL = """
      No settings found at {} and no SETTINGS_URL is defined.
      Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
      Please contact the technical lead of your project to get the SETTINGS_URL for your project to enter.
      In case you just want to test IDEasy you may simply hit return to install the default settings.""";

  private static final String MESSAGE_INVALID_REPOSITORY = "Settings repository integrity check failed: "
      + "The given git repository URL does not point to a valid settings or code-settings repository. Please verify and try again.";

  private final IdeContext context;

  private final StringProperty settingsRepoProperty;

  /** The temporary directory holding the verified clone or {@code null} if there is nothing to move. */
  private Path tempDir;

  /** The name of the git project - required to place a combined code and settings repository into the workspace. */
  private String gitProjectName;

  /**
   * Status of the settings {@link SettingsUpdater#checkSettings() health check} describing what {@link SettingsUpdater#applySettings(SettingsUpdateResult)} has
   * to do.
   */
  public enum ResultStatus {
    /** The settings repository was already present and is valid - it only has to be pulled in place. */
    SETTINGS_UPDATED,
    /** The settings repository was cloned to a temporary directory and is valid - it has to be moved to its final location. */
    SETTINGS_CLONED,
    /** The settings could not be updated but the settings already present are still valid so the process can continue without updating them. */
    SETTINGS_UPDATE_FAILED
  }

  /**
   * Result of the settings {@link SettingsUpdater#checkSettings() health check}.
   *
   * @param status the {@link ResultStatus}.
   * @param repositoryType the {@link RepositoryType} of the settings repository.
   * @param errorMessage the reason why the settings could not be updated or {@code null} if the health check succeeded.
   */
  public record SettingsUpdateResult(ResultStatus status, RepositoryType repositoryType, String errorMessage) {

    /**
     * @param status the {@link ResultStatus}.
     * @param repositoryType the {@link RepositoryType}.
     * @return a {@link SettingsUpdateResult} for a successful health check.
     */
    static SettingsUpdateResult of(ResultStatus status, RepositoryType repositoryType) {

      return new SettingsUpdateResult(status, repositoryType, null);
    }

    /**
     * @param repositoryType the {@link RepositoryType} of the settings that are already present.
     * @param errorMessage the reason why the settings could not be updated.
     * @return a {@link SettingsUpdateResult} for a failed but recoverable health check.
     */
    static SettingsUpdateResult failed(RepositoryType repositoryType, String errorMessage) {

      return new SettingsUpdateResult(ResultStatus.SETTINGS_UPDATE_FAILED, repositoryType, errorMessage);
    }
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param settingsRepoProperty the {@link StringProperty} with the settings repository URL from the update commandlet.
   */
  public SettingsUpdater(IdeContext context, StringProperty settingsRepoProperty) {

    super();
    this.context = context;
    this.settingsRepoProperty = settingsRepoProperty;
  }

  /**
   * Performs the health check on the settings repository. Nothing is changed in {@link IdeContext#getIdeHome() IDE_HOME} except that a broken settings folder
   * is backed up. Whether the settings are pulled or cloned is decided solely by the state of {@link IdeContext#getSettingsPath() IDE_HOME/settings} so that
   * {@code ide create} and {@code ide update} share the very same logic.
   *
   * @return the {@link SettingsUpdateResult}.
   */
  public SettingsUpdateResult checkSettings() {

    Path settingsPath = this.context.getSettingsPath();
    if (settingsPath != null) {
      // for a combined code and settings repository IDE_HOME/settings is a symlink into the code repository whose '.git' folder is one level above,
      // so isGitRepo would report it as broken settings
      boolean codeRepository = this.context.isSettingsCodeRepository();
      if (codeRepository || this.context.getGitContext().isGitRepo(settingsPath)) {
        return checkPresentSettings(settingsPath, codeRepository ? RepositoryType.CODE_SETTINGS_COMBINED : RepositoryType.SETTINGS);
      }
    }
    return checkClonedSettings(settingsPath);
  }

  /**
   * Applies the result of the {@link #checkSettings() health check} by either pulling the settings in place or moving the verified clone to its final
   * location.
   *
   * @param result the {@link SettingsUpdateResult} from {@link #checkSettings()}.
   */
  public void applySettings(SettingsUpdateResult result) {

    switch (result.status()) {
      case SETTINGS_UPDATED -> pullSettings();
      case SETTINGS_CLONED -> moveSettings(result.repositoryType());
      case SETTINGS_UPDATE_FAILED -> LOG.error("Settings repository has not been updated: {}", result.errorMessage());
    }
  }

  /**
   * Health check for settings that are already present. As the project keeps working with these settings, a failure is only fatal if the user explicitly
   * aborted.
   */
  private SettingsUpdateResult checkPresentSettings(Path settingsPath, RepositoryType repositoryType) {

    try {
      GitUrl gitUrl = GitUrl.of(this.context.getGitContext().retrieveGitUrl(settingsPath));
      RepositoryType clonedType = RepositoryUtil.getRepositoryType(cloneToTempDir(gitUrl));
      deleteTempDir();
      if (!isSettingsRepository(clonedType) && !confirmInvalidRepository(clonedType, gitUrl)) {
        return SettingsUpdateResult.failed(repositoryType, MESSAGE_INVALID_REPOSITORY);
      }
      return SettingsUpdateResult.of(ResultStatus.SETTINGS_UPDATED, repositoryType);
    } catch (RuntimeException e) {
      deleteTempDir();
      if (e instanceof CliAbortException) {
        // the user answered "no" so we must not silently carry on
        throw toFatalException(e);
      }
      return SettingsUpdateResult.failed(repositoryType, e.getMessage());
    }
  }

  /**
   * Health check for missing or broken settings. Without valid settings there is nothing to continue with, so every failure is fatal here.
   */
  private SettingsUpdateResult checkClonedSettings(Path settingsPath) {

    try {
      backupBrokenSettings(settingsPath);
      GitUrl gitUrl = getOrAskSettingsUrl();
      RepositoryType repositoryType = RepositoryUtil.getRepositoryType(cloneToTempDir(gitUrl));
      if (!isSettingsRepository(repositoryType) && !confirmInvalidRepository(repositoryType, gitUrl)) {
        throw new CliRethrowException(MESSAGE_INVALID_REPOSITORY);
      }
      return SettingsUpdateResult.of(ResultStatus.SETTINGS_CLONED, repositoryType);
    } catch (RuntimeException e) {
      deleteTempDir();
      throw toFatalException(e);
    }
  }

  /**
   * @param error the {@link RuntimeException} that made the settings setup fail.
   * @return a {@link CliRethrowException} that aborts the entire process. An existing {@link CliException} keeps its message and
   *     {@link CliException#getExitCode() exit code} so that e.g. an abort by the user is still reported as such.
   */
  private static CliRethrowException toFatalException(RuntimeException error) {

    if (error instanceof CliRethrowException rethrow) {
      return rethrow;
    } else if (error instanceof CliException) {
      return new CliRethrowException(error.getMessage(), error);
    }
    return new CliRethrowException("Failed to set up the settings repository: " + error.getMessage(), error);
  }

  private void pullSettings() {

    Path settingsPath = this.context.getSettingsPath();
    GitContext gitContext = this.context.getGitContext();
    if (gitContext.hasUntrackedFiles(settingsPath)) {
      gitContext.pullSafelyWithStash(settingsPath);
    } else {
      gitContext.pull(settingsPath);
    }
    gitContext.saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
  }

  private void moveSettings(RepositoryType repositoryType) {

    Path settingsPath = this.context.getSettingsPath();
    if ((repositoryType == RepositoryType.SETTINGS) || (repositoryType == RepositoryType.UNKNOWN)) {
      moveProject(this.tempDir, settingsPath);
      this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
    } else {
      // for a code repository we clone into the workspace and symlink IDE_HOME/settings to its settings folder
      Path codePath = this.context.getWorkspacePath().resolve(this.gitProjectName);
      moveProject(this.tempDir, codePath);
      Path settingsFolder = codePath.resolve(IdeContext.FOLDER_SETTINGS);
      if (Files.isDirectory(settingsFolder)) {
        this.context.getFileAccess().symlink(settingsFolder, settingsPath);
        this.context.getGitContext().saveCurrentCommitId(settingsFolder, this.context.getSettingsCommitIdPath());
      } else {
        LOG.warn("The repository has been cloned to {} but it does not contain a settings folder so your project has no settings.", codePath);
      }
    }
    this.tempDir = null;
  }

  private Path cloneToTempDir(GitUrl gitUrl) {

    this.gitProjectName = gitUrl.getProjectName();
    // createTempDir guarantees a unique and empty directory so no leftovers of a previous attempt can interfere and we can clone directly
    this.tempDir = this.context.getFileAccess().createTempDir(this.gitProjectName + "-");
    this.context.getGitContext().clone(gitUrl, this.tempDir);
    return this.tempDir;
  }

  private void backupBrokenSettings(Path settingsPath) {

    if ((settingsPath == null) || !Files.exists(settingsPath)) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    if (!fileAccess.isEmptyDir(settingsPath)) {
      this.context.askToContinue("""
          Your settings repository seems to be broken ('.git' folder not present).
          We can fix this by moving your settings to the backup.
          You will be asked for the settings git URL and your settings will be cloned from scratch.
          Do you want to proceed?""");
    }
    fileAccess.backup(settingsPath);
  }

  /**
   * @return {@code true} if the user explicitly wants to continue with an invalid repository, {@code false} otherwise.
   */
  private boolean confirmInvalidRepository(RepositoryType repositoryType, GitUrl gitUrl) {

    if (!this.context.isForceMode()) {
      return false;
    }
    LOG.warn("{}\nURL: {}\nDetected repository type: {}", MESSAGE_INVALID_REPOSITORY, gitUrl, repositoryType);
    this.context.askToContinue("Force mode is active. Do you want to continue anyway?");
    return true;
  }

  private static boolean isSettingsRepository(RepositoryType repositoryType) {

    return (repositoryType == RepositoryType.SETTINGS) || (repositoryType == RepositoryType.CODE_SETTINGS_COMBINED);
  }

  /**
   * Releases the temporary clone if it has not been moved to its final location. The clone is created by {@link #checkSettings()} and consumed by
   * {@link #applySettings(SettingsUpdateResult)}, so its lifetime spans both phases and has to be ended by the caller once it is done with them.
   */
  public void cleanup() {

    deleteTempDir();
  }

  /**
   * Removes the temporary clone. It is deleted and not backed up since it only contains a fresh clone without any user data and a backup would be created
   * inside {@link IdeContext#getIdeHome() IDE_HOME} that may not even exist yet. Failures are only logged so that the actual error never gets masked.
   */
  private void deleteTempDir() {

    if (this.tempDir == null) {
      return;
    }
    try {
      this.context.getFileAccess().delete(this.tempDir);
    } catch (RuntimeException e) {
      LOG.warn("Failed to delete temporary directory {}", this.tempDir, e);
    }
    this.tempDir = null;
  }

  private GitUrl getOrAskSettingsUrl() {

    String repository = handleDefaultRepository(this.settingsRepoProperty.getValue());
    GitUrl gitUrl = null;
    if (repository != null) {
      gitUrl = GitUrl.of(repository);
    }
    if ((gitUrl == null) || !gitUrl.isValid()) {
      LOG.info(MESSAGE_SETTINGS_REPO_URL, this.context.getSettingsPath());
    }
    String userPrompt = "Settings URL [" + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
    while ((gitUrl == null) || !gitUrl.isValid()) {
      repository = handleDefaultRepository(this.context.askForInput(userPrompt, IdeContext.DEFAULT_SETTINGS_REPO_URL));
      gitUrl = GitUrl.of(repository);
      if (!gitUrl.isValid()) {
        LOG.warn("The input URL is not valid, please try again.");
      }
    }
    return gitUrl;
  }

  private String handleDefaultRepository(String repository) {

    if ("-".equals(repository)) {
      LOG.info("'-' was found for the repository, the default settings repository '{}' will be used.", IdeContext.DEFAULT_SETTINGS_REPO_URL);
      repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    }
    return repository;
  }

  private void moveProject(Path from, Path to) {

    try {
      this.context.getFileAccess().move(from, to);
    } catch (RuntimeException e) {
      // FileAccess already reports source, target and the Windows file-lock hint so we only escalate to a fatal error here
      throw new CliRethrowException(e.getMessage(), e);
    }
  }
}
