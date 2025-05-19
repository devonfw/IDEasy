package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to print a status report about IDEasy.
 */
public class StatusCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public StatusCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "status";
  }

  @Override
  public void run() {
    Step step = this.context.newStep(true, "Show IDE_ROOT and IDE_HOME");
    step.run(this.context::logIdeHomeAndRootStatus);
    step = this.context.newStep(true, "Show online status");
    step.run(this::logOnlineStatus);

    if (this.context.getIdeHome() != null) {
      step = this.context.newStep(true, "Show git status");
      step.run(this::logSettingsGitStatus);
      step = this.context.newStep(true, "Show legacy status");
      step.run(this::logSettingsLegacyStatus);
      step = this.context.newStep(true, "Show migration status");
      step.run(this::logMigrationStatus);
    }
    step = this.context.newStep(true, "Check for updates of IDEasy");
    step.run(this::checkForUpdate);
  }

  private void checkForUpdate() {
    if (!this.context.isOnline()) {
      this.context.warning("Check for newer version of IDEasy is skipped due to no network connectivity.");
      return;
    }
    new IdeasyCommandlet(this.context, null).checkIfUpdateIsAvailable();
    logSystemInfo();
  }

  private void logSystemInfo() {
    SystemInfo systemInfo = this.context.getSystemInfo();
    this.context.info("Your operating system is {}({})@{} [{}@{}]", systemInfo.getOs(), systemInfo.getOsVersion(), systemInfo.getArchitecture(),
        systemInfo.getOsName(), systemInfo.getArchitectureName());
  }

  private void logSettingsLegacyStatus() {
    EnvironmentVariables variables = this.context.getVariables();
    boolean hasLegacyProperties = false;
    while (variables != null) {
      Path legacyProperties = variables.getLegacyPropertiesFilePath();
      if (legacyProperties != null && Files.exists(legacyProperties)) {
        hasLegacyProperties = true;
        this.context.warning("Found legacy properties {}", legacyProperties);
      }
      variables = variables.getParent();
    }
    if (hasLegacyProperties) {
      this.context.warning(
          "Your settings are outdated and contain legacy configurations. Please consider upgrading your settings:\nhttps://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc#upgrade");
    }
  }

  private void logSettingsGitStatus() {
    Path settingsPath = this.context.getSettingsGitRepository();
    if (settingsPath == null) {
      if (this.context.getIdeHome() != null) {
        this.context.error("No settings repository was found.");
      }
    } else {
      GitContext gitContext = this.context.getGitContext();
      if (gitContext.isRepositoryUpdateAvailable(settingsPath, this.context.getSettingsCommitIdPath())) {
        if (!this.context.isSettingsRepositorySymlinkOrJunction()) {
          this.context.warning("Your settings are not up-to-date, please run 'ide update'.");
        }
      } else {
        this.context.success("Your settings are up-to-date.");
      }
      String branch = gitContext.determineCurrentBranch(settingsPath);
      this.context.debug("Your settings branch is {}", branch);
      if (!"master".equals(branch) && !"main".equals(branch)) {
        this.context.warning("Your settings are on a custom branch: {}", branch);
      }
    }
  }

  private void logOnlineStatus() {
    if (this.context.isOfflineMode()) {
      this.context.warning("You have configured offline mode via CLI.");
    } else if (this.context.isOnline()) {
      this.context.success("You are online.");
    } else {
      this.context.warning("You are offline. Check your internet connection and potential proxy settings.");
    }
  }

  private void logMigrationStatus() {

    IdeMigrator migrator = new IdeMigrator();
    VersionIdentifier projectVersion = this.context.getProjectVersion();
    VersionIdentifier targetVersion = migrator.getTargetVersion();
    if (projectVersion.isLess(targetVersion)) {
      this.context.interaction("Your project is on IDEasy version {} and needs an update to version {}!\nPlease run 'ide update' to migrate your project",
          projectVersion, targetVersion);
    }
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }
}
