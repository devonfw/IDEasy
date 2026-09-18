package com.devonfw.ide.gui.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.tools.ide.commandlet.UpgradeCommandlet;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Encapsulates the tool-wide IDEasy upgrade business logic (checking for and running upgrades), independent of any UI framework and independent of any selected
 * project/workspace.
 */
public class UpgradeService {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeService.class);

  private final GuiStateManager manager;

  private String installedVersion = "";
  private String latestVersion = "";

  /**
   * The constructor.
   *
   * @param manager the {@link GuiStateManager} used to obtain the shared start context.
   */
  public UpgradeService(GuiStateManager manager) {

    this.manager = manager;
  }

  /**
   * Performs the actual upgrade availability check. Also resolves the installed/latest version strings as a side effect, retrievable via
   * {@link #getInstalledVersion()} and {@link #getLatestVersion()}.
   *
   * @return true if an upgrade is available, false otherwise
   */
  public boolean checkForUpgrade() {
    if (IdeVersion.isUndefined()) {
      return false;
    }
    try {
      IdeGuiContext ctx = new IdeGuiContext(this.manager.getStartContext(), null, manager.getTaskManager());
      IdeasyCommandlet cmd = new IdeasyCommandlet(ctx, null);
      try {
        var installed = cmd.getInstalledVersion();
        var latest = cmd.getLatestVersion();
        this.installedVersion = installed == null ? "" : installed.toString();
        this.latestVersion = latest == null ? "" : latest.toString();
      } catch (Exception e) {
        LOG.debug("Failed to resolve versions", e);
        this.installedVersion = "";
        this.latestVersion = "";
      }

      return cmd.checkIfUpdateIsAvailable();
    } catch (Exception e) {
      LOG.debug("Upgrade check failed", e);
      return false;
    }
  }

  /**
   * Runs the IDEasy upgrade commandlet.
   */
  public void runUpgrade() {

    IdeGuiContext ctx = new IdeGuiContext(this.manager.getStartContext(), null, manager.getTaskManager());
    new UpgradeCommandlet(ctx).run();
  }

  /**
   * @return the installed version resolved by the last {@link #checkForUpgrade()} call, or {@code ""} if unknown.
   */
  public String getInstalledVersion() {

    return this.installedVersion;
  }

  /**
   * @return the latest available version resolved by the last {@link #checkForUpgrade()} call, or {@code ""} if unknown.
   */
  public String getLatestVersion() {

    return this.latestVersion;
  }
}
