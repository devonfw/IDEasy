package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to print a status report about IDEasy.
 */
public class StatusCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(StatusCommandlet.class);

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
  protected boolean isActivateJaveUtilLogging() {

    return false;
  }

  @Override
  protected void doRun() {
    Step step = this.context.newStep(true, "Show IDE_ROOT and IDE_HOME");
    step.run(this.context::logIdeHomeAndRootStatus);
    step = this.context.newStep(true, "Check for updates of IDEasy");
    step.run(this::checkForUpdate);
    step = this.context.newStep(true, "Show online status");
    step.run(this::logOnlineStatus);
    step = this.context.newStep(true, "Show git and bash location");
    step.run(this::logGitBashLocationStatus);

    if (this.context.getIdeHome() != null) {
      step = this.context.newStep(true, "Show git status");
      step.run(this::logSettingsGitStatus);
      step = this.context.newStep(true, "Show legacy status");
      step.run(this::logSettingsLegacyStatus);
      step = this.context.newStep(true, "Show migration status");
      step.run(this::logMigrationStatus);
    }
  }

  private void checkForUpdate() {
    new IdeasyCommandlet(this.context, null).checkIfUpdateIsAvailable();
    logSystemInfo();
  }

  private void logSystemInfo() {
    SystemInfo systemInfo = this.context.getSystemInfo();
    LOG.info("Your operating system is {}({})@{} [{}@{}]", systemInfo.getOs(), systemInfo.getOsVersion(), systemInfo.getArchitecture(),
        systemInfo.getOsName(), systemInfo.getArchitectureName());
  }

  private void logSettingsLegacyStatus() {
    EnvironmentVariables variables = this.context.getVariables();
    boolean hasLegacyProperties = false;
    while (variables != null) {
      Path legacyProperties = variables.getLegacyPropertiesFilePath();
      if (legacyProperties != null && Files.exists(legacyProperties)) {
        hasLegacyProperties = true;
        LOG.warn("Found legacy properties {}", legacyProperties);
      }
      variables = variables.getParent();
    }
    if (hasLegacyProperties) {
      LOG.warn(
          "Your settings are outdated and contain legacy configurations. Please consider upgrading your settings:\nhttps://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc#upgrade");
    }
  }

  private void logSettingsGitStatus() {
    Path settingsPath = this.context.getSettingsGitRepository();
    if (settingsPath == null) {
      if (this.context.getIdeHome() != null) {
        LOG.error("No settings repository was found.");
      }
    } else {
      GitContext gitContext = this.context.getGitContext();
      if (gitContext.isRepositoryUpdateAvailable(settingsPath, this.context.getSettingsCommitIdPath())) {
        if (!this.context.isSettingsRepositorySymlinkOrJunction()) {
          LOG.warn("Your settings are not up-to-date, please run 'ide update'.");
        }
      } else {
        LOG.info(IdeLogLevel.SUCCESS.getSlf4jMarker(), "Your settings are up-to-date.");
      }
      String branch = gitContext.determineCurrentBranch(settingsPath);
      LOG.debug("Your settings branch is {}", branch);
      if (!"master".equals(branch) && !"main".equals(branch)) {
        LOG.warn("Your settings are on a custom branch: {}", branch);
      }
    }
  }

  private void logOnlineStatus() {
    this.context.getNetworkStatus().logStatusMessage();
  }

  private void logMigrationStatus() {

    IdeMigrator migrator = new IdeMigrator();
    VersionIdentifier projectVersion = this.context.getProjectVersion();
    VersionIdentifier targetVersion = migrator.getTargetVersion();
    if (projectVersion.isLess(targetVersion)) {
      LOG.info(IdeLogLevel.INTERACTION.getSlf4jMarker(),
          "Your project is on IDEasy version {} and needs an update to version {}!\nPlease run 'ide update' to migrate your project",
          projectVersion, targetVersion);
    }
  }

  private void logGitBashLocationStatus() {
    Path bashPath = this.context.findBash();
    if (bashPath != null) {
      LOG.info(IdeLogLevel.SUCCESS.getSlf4jMarker(), "Found bash executable at: {}", bashPath);
    } else {
      LOG.error("No bash executable was found on your system!");
    }
    GitContext gitContext = this.context.getGitContext();
    Path gitPath = gitContext.findGit();
    if (gitPath != null) {
      LOG.info(IdeLogLevel.SUCCESS.getSlf4jMarker(), "Found git executable at: {}", gitPath);
    } else {
      LOG.error("No git executable was found on your system!");
    }
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }
}
