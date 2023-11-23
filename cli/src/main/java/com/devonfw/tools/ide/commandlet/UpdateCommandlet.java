package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;

import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateCommandlet extends Commandlet {

  public final StringProperty settingsURL;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    settingsURL = add(new StringProperty("", false, "settingsURL"));

  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  public void run() {

    updateSettings();
    updateSoftware();
    updateRepositories();

  }

  private void updateRepositories() {

  }

  private void updateSoftware() {

  }

  private void updateSettings() {

    Path settingsPath = this.context.getSettingsPath();
    if (Files.isDirectory(settingsPath)) {
      //pull remote settings repo
      this.context.gitPullOrClone(settingsPath, "http");
    } else {
      //clone given settingsUrl
      String settingsUrl = this.settingsURL.getValue();
      if (settingsUrl != null ) {

      }

    }



  }
}
