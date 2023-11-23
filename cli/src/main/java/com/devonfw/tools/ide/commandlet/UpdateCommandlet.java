package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;

import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateCommandlet extends Commandlet {

  public final StringProperty settingsURL;

  private static final String defaultSettingsUrl = "https://github.com/devonfw/ide-settings.git";

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
      if (settingsUrl == null) {
        if (this.context.isBatchMode()) {
          settingsUrl = "";
        } else {
          this.context.info("Missing your settings at {} and no Settings URL is defined.");
          this.context.info("Further details can be found here:");
          this.context.info("https://github.com/devonfw/IDEasy/blob/main/documentation/settings.asciidoc");
          this.context.info("Please contact the technical lead of your project to get the SETTINGS_URL for your project.");
          this.context.info("In case you just want to test IDEasy you may simply hit return to install default settings.");
          //read line settingsUrl = this.context.readLine();
        }
      }

      if (settingsUrl.isEmpty()) {
        settingsUrl = defaultSettingsUrl;
      }
      this.context.gitPullOrClone(settingsPath, settingsUrl);

    }



  }
}
