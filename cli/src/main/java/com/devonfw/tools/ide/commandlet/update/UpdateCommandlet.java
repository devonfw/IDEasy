package com.devonfw.tools.ide.commandlet.update;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.UpgradeSettingsCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.migration.IdeMigrator;

/**
 * {@link AbstractCommandlet} to update settings, software and repositories
 */
public class UpdateCommandlet extends AbstractUpdateCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateCommandlet.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  protected void doRun() {
    new IdeMigrator().run(this.context);
    super.doRun();
    warnAboutObsoleteWorkspaceTemplates();
  }

  /**
   * Warns if the (just updated) settings still contain obsolete workspace templates (e.g. {@code idea.properties}, see #2531), since only the IDEasy admin can
   * remove them from the settings repository via {@link UpgradeSettingsCommandlet}.
   */
  private void warnAboutObsoleteWorkspaceTemplates() {

    List<Path> obsoleteTemplates = this.context.getCommandletManager().getCommandlet(UpgradeSettingsCommandlet.class).findObsoleteWorkspaceTemplates();
    if (obsoleteTemplates.isEmpty()) {
      return;
    }
    for (Path template : obsoleteTemplates) {
      LOG.warn("Found obsolete template {} in your settings that is not used anymore since IDEasy now generates this file itself.", template);
    }
    IdeLogLevel.INTERACTION.log(LOG, "Please ask the IDEasy admin of your project to run the following command and commit the changes to your settings:\n"
        + "ide upgrade-settings");
  }
}
