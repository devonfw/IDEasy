package com.devonfw.tools.ide.commandlet.update.settings;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliFatalException;
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
 * <li>{@link #checkSettings(Path)} health check: the settings are always cloned into a temporary directory
 * first where it is verified that the git URL is valid, that cloning succeeded,
 * and that the repository actually is a settings or a combined code and settings repository.</li>
 * <li>{@link #applySettings(boolean, Path)} apply: only after the health check succeeded the settings are either pulled in place (if they were already
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

  private final FileAccess fileAccess;

  private final StringProperty settingsRepoProperty;

  /** The temporary directory holding the verified clone or {@code null} if there is nothing to move. */
  private Path tempRepoDir;

  /** The name of the git project - required to place a combined code and settings repository into the workspace. */
  private String gitProjectName;

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
    this.fileAccess = context.getFileAccess();
  }

  /**
   * Performs the health check on the settings repository. Nothing is changed in {@link IdeContext#getIdeHome() IDE_HOME} except that a broken settings folder
   * is backed up. Whether the settings are pulled or cloned is decided solely by the state of {@link IdeContext#getSettingsPath() IDE_HOME/settings} so that
   * {@code ide create} and {@code ide update} share the very same logic.
   *
   * @param settingsPath the path to the (code-)settings directory which the health check should be performed on.
   * @return the {@link SettingsHealthCheckResult}.
   */
  public SettingsHealthCheckResult checkSettings(Path settingsPath) {

    if (settingsPath != null && !fileAccess.isEmptyDir(settingsPath)) {
      // for a combined code and settings repository IDE_HOME/settings is a symlink into the code repository whose '.git' folder is one level above,
      // so isGitRepo would report it as broken settings
      RepositoryType settingsRepoType = RepositoryUtil.getRepositoryType(settingsPath);
      if (settingsRepoType.isSettingsOrCodeSettingsRepository()) {
        return checkSettingsPresent(settingsPath, settingsRepoType);
      }
    }
    return checkClonedSettings(settingsPath);
  }

  /**
   * Applies the result of the {@link #checkSettings(Path)} health check by either pulling the settings in place or moving the verified clone to its final
   * location.
   *
   * @param onlyPull if true, we simply perform a git pull on the actual (not the one in the temp directory) settings repository.
   * @param sourcePath sourcePath of the settings to apply.
   * @return a {@link SettingsUpdateResult} representing the state, whether moving/pulling the newest settings was successful.
   */
  public SettingsUpdateResult applySettings(boolean onlyPull, Path sourcePath) {

    RepositoryType repositoryType = RepositoryUtil.getRepositoryType(sourcePath);
    Path settingsPath = this.context.getSettingsPath();

    // Case 1: We performed "ide update"; so settings already existed and we just need to perform a git pull in the existing repo.
    if (onlyPull) {
      repositoryType = RepositoryUtil.getRepositoryType(context.getSettingsPath());
      if (repositoryType != RepositoryType.SETTINGS) {
        return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_UPDATE_FAILED,
            repositoryType,
            "Expected settings repository for update application, but was of type: " + repositoryType);
      }

      pullSettingsAndSaveCommitId(settingsPath);
      return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_UPDATED, repositoryType, null);
    }

    // Case 2: We freshly cloned the settings repo and need to move it to a target directory.
    switch (repositoryType) {
      case PLAIN_CODE, UNKNOWN -> {

        return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_UPDATE_FAILED, repositoryType,
            "Cannot apply settings as type of the settings repo is incorrect");
      }
      case SETTINGS -> {

        //move to IDE_HOME/SETTINGS
        moveProject(sourcePath, settingsPath);
        this.context.getGitContext().saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
        return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_CLONED, repositoryType, null);
      }
      case CODE_SETTINGS_COMBINED -> {

        //this is a special case - here we need to symlink from IDE_HOME/settings to IDE_HOME/workspaces/main/repo_name/settings.
        // (Formerly managed by the obsolete "--code" flag)
        Path repoMoveTargetDirectory = this.context.getWorkspacePath().resolve(gitProjectName);
        Path symlinkPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_SETTINGS);
        Path repoSettingsDirectory = repoMoveTargetDirectory.resolve(IdeContext.FOLDER_SETTINGS);

        moveProject(sourcePath, repoMoveTargetDirectory);

        context.getFileAccess().symlink(repoSettingsDirectory, symlinkPath);

        this.context.getGitContext().saveCurrentCommitId(repoSettingsDirectory, this.context.getSettingsCommitIdPath());
        return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_CLONED, repositoryType, null);
      }
    }

    return new SettingsUpdateResult(SettingsUpdateStatus.SETTINGS_UPDATE_FAILED, repositoryType, "Unknown error during settings");
  }

  /**
   * Health check for settings that are already present. As the project keeps working with these settings, a failure is only fatal if the user explicitly
   * aborted. Here, if a new version is available, we clone the new version into a temporary folder and perform health checks. If the cloned, new version is
   * valid, we call git update in the existing settings folder.
   */
  private SettingsHealthCheckResult checkSettingsPresent(Path settingsPath, RepositoryType repositoryType) {

    try {
      //Get Git url of existing settings, clone newest version of them to temp dir
      GitUrl gitUrl = GitUrl.of(this.context.getGitContext().retrieveGitUrl(settingsPath));
      RepositoryType clonedType = RepositoryUtil.getRepositoryType(cloneRepoToTempDir(gitUrl));
      cleanup();

      //If cloned repo is not (code-)settings repo and no force override (e.g. force mode) is applied, return error.
      if (!clonedType.isSettingsOrCodeSettingsRepository() && !requestUserConfirmInvalidRepository(clonedType, gitUrl, true)) {
        return SettingsHealthCheckResult.failed(clonedType, MESSAGE_INVALID_REPOSITORY, settingsPath);
      }

      //Otherwise, (e.g. user overrides), return valid.
      return SettingsHealthCheckResult.of(HealthCheckResultStatus.SETTINGS_VALID_EXISTING, repositoryType, settingsPath);
    } catch (RuntimeException e) {
      cleanup();
      if (e instanceof CliAbortException) {
        // the user answered "no" so we must not silently carry on
        return SettingsHealthCheckResult.failed(repositoryType, "Settings update aborted by end-user", settingsPath);
      }
      return SettingsHealthCheckResult.failed(repositoryType, e.getMessage(), settingsPath);
    }
  }

  /**
   * Health check for missing or broken settings (e.g. {@code ide create}). Without valid settings there is nothing to continue with, so every failure is fatal
   * here.
   */
  private SettingsHealthCheckResult checkClonedSettings(Path settingsPath) {

    try {
      backupBrokenSettings(settingsPath);
      GitUrl gitUrl = getOrAskSettingsUrl();

      Path tempCloneDir = cloneRepoToTempDir(gitUrl);
      RepositoryType repositoryType = RepositoryUtil.getRepositoryType(tempCloneDir);

      if (!repositoryType.isSettingsOrCodeSettingsRepository() && !requestUserConfirmInvalidRepository(repositoryType, gitUrl, false)) {
        //see @javadoc why we throw fatally here.
        throw new CliFatalException(MESSAGE_INVALID_REPOSITORY);
      }
      return SettingsHealthCheckResult.of(HealthCheckResultStatus.SETTINGS_VALID, repositoryType, tempCloneDir);
    } catch (RuntimeException e) {
      cleanup();
      throw createGuaranteedFatalException(e);
    }
  }

  /**
   * @param error the {@link RuntimeException} that made the settings setup fail.
   * @return a {@link CliFatalException} that aborts the entire process. An existing {@link CliException} keeps its message and
   *     {@link CliException#getExitCode() exit code} so that e.g. an abort by the user is still reported as such.
   */
  private static CliFatalException createGuaranteedFatalException(RuntimeException error) {

    if (error instanceof CliFatalException rethrow) {
      return rethrow;
    } else if (error instanceof CliException) {
      return new CliFatalException(error.getMessage(), error);
    }
    return new CliFatalException("Error occurred during settings update: " + error.getClass() + ": " + error.getMessage(), error);
  }

  private void pullSettingsAndSaveCommitId(Path settingsPath) {

    GitContext gitContext = this.context.getGitContext();
    if (gitContext.hasUntrackedFiles(settingsPath)) {
      gitContext.pullSafelyWithStash(settingsPath);
    } else {
      gitContext.pull(settingsPath);
    }
    gitContext.saveCurrentCommitId(settingsPath, this.context.getSettingsCommitIdPath());
  }

  /**
   * Clone a settings repository into a temporary directory.
   *
   * @param gitUrl {@link GitUrl} of the (code-)settings repository.
   * @return {@link Path} of the temporary directory.
   */
  private Path cloneRepoToTempDir(GitUrl gitUrl) {

    this.gitProjectName = gitUrl.getProjectName();

    // createTempDir guarantees a unique and empty directory so no leftovers of a previous attempt can interfere and we can clone directly
    this.tempRepoDir = this.context.getFileAccess().createTempDir("project-" + this.gitProjectName);
    this.context.getGitContext().clone(gitUrl, this.tempRepoDir);
    return this.tempRepoDir;
  }

  private void backupBrokenSettings(Path settingsPath) {

    if ((settingsPath == null) || !Files.exists(settingsPath)) {
      return;
    }

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
  private boolean requestUserConfirmInvalidRepository(RepositoryType repositoryType, GitUrl gitUrl, boolean updatesExistingRepository) {

    LOG.warn("{}\nURL: {}\nDetected settings repository type: {}", MESSAGE_INVALID_REPOSITORY, gitUrl, repositoryType);

    this.context.askToContinue("The update to the settings repository you are trying to apply seems to be broken. Do you want to continue anyway?");
    return true;
  }

  /**
   * Removes the temporary clone. It is deleted and not backed up since it only contains a fresh clone without any user data and a backup would be created
   * inside {@link IdeContext#getIdeHome() IDE_HOME} that may not even exist yet. Failures are only logged so that the actual error never gets masked.
   */
  public void cleanup() {

    if (this.tempRepoDir == null) {
      return;
    }
    try {
      this.context.getFileAccess().delete(this.tempRepoDir);
    } catch (RuntimeException e) {
      LOG.warn("Failed to delete temporary directory {}", this.tempRepoDir, e);
    }
    this.tempRepoDir = null;
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
      throw new CliFatalException(e.getMessage(), e);
    }
  }
}
