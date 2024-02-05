package com.devonfw.tools.ide.tool.npm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */
// TODO
public class Npm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "node", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  public static void doSetup() {

    String softwareDir = System.getenv("NODE_HOME") + "tool/npm/Npm";
    // TODO hardcoded
    boolean success = true;

    if (success) {
      deleteFile(System.getenv("NODE_HOME") + "/npm");
      deleteFile(System.getenv("NODE_HOME") + "/npm.cmd");
      deleteFile(System.getenv("NODE_HOME") + "/npx");
      deleteFile(System.getenv("NODE_HOME") + "/npx.cmd");

      copyFile(softwareDir + "/bin/npm", System.getenv("NODE_HOME"));
      copyFile(softwareDir + "/bin/npm.cmd", System.getenv("NODE_HOME"));
      copyFile(softwareDir + "/bin/npx", System.getenv("NODE_HOME"));
      copyFile(softwareDir + "/bin/npx.cmd", System.getenv("NODE_HOME"));
    }
  }

  private static void deleteFile(String filePath) {

    File file = new File(filePath);
    if (file.exists()) {
      file.delete();
    }
  }

  private static void copyFile(String source, String destination) {

    Path sourcePath = Path.of(source);
    Path destinationPath = Path.of(destination);

    try {
      Files.copy(sourcePath, destinationPath.resolve(sourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
