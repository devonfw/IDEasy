package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    List<Path> test = context.getFileAccess().listChildrenRecursive(source, path -> path.getFileName().toString().equals("devon.properties"));
    for (Path file_path : test) {

      Path target = file_path.getParent().resolve("ide.properties");

      try {
        Files.move(file_path, target);
        this.context.success("updated file name: " + file_path + "\n-> " + target);
      } catch (IOException e) {
        this.context.error("Error updating file name: " + file_path);
      }
    }
  }
}
