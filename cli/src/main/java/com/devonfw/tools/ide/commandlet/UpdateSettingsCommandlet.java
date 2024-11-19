package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

public class UpdateSettingsCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateSettingsCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "update-settings";
  }

  @Override
  public void run() {
    Path source = context.getIdeHome();
    if (context.getFileAccess().findFirst(source, path -> path.getFileName().toString().equals("devon.properties"), true)) {
      this.context.info("hi");
    } else {
      this.context.info("nein");
    }
  }
}
