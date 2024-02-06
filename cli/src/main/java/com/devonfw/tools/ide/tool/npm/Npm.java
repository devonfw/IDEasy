package com.devonfw.tools.ide.tool.npm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */

public class Npm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  @Override
  public void run() {

    runNpm(false);
    doSetup();
  }

  protected ProcessResult runNpm(boolean log, String... args) {

    Path toolPath = getToolPath();
    ProcessContext pc = this.context.newProcess();
    if (log) {
      pc.errorHandling(ProcessErrorHandling.ERROR);
    }
    pc.executable(toolPath);
    return pc.run(log);
  }

  public void doSetup() {

    String TOOL_PATH = getToolPath().toString();
    String softwareDir = TOOL_PATH + "node_modules/npm";

    boolean success = true;

    if (success) {
      deleteFile(TOOL_PATH + "/npm");
      deleteFile(TOOL_PATH + "/npm.cmd");
      deleteFile(TOOL_PATH + "/npx");
      deleteFile(TOOL_PATH + "/npx.cmd");

      copyFile(softwareDir + "/bin/npm", TOOL_PATH);
      copyFile(softwareDir + "/bin/npm.cmd", TOOL_PATH);
      copyFile(softwareDir + "/bin/npx", TOOL_PATH);
      copyFile(softwareDir + "/bin/npx.cmd", TOOL_PATH);
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

  public String getProjectVersion() throws IOException {

    final File PACKAGE_JSON = new File(getToolPath().toString() + "node_modules/npm/package.json");
    if (PACKAGE_JSON.isFile()) {
      BufferedReader reader = new BufferedReader(new FileReader(PACKAGE_JSON));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("\"version\"")) {
          Pattern pattern = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
            return matcher.group(1);
          } else {
            throw new IOException("Could not determine version from package.json");
          }
        }
      }
      throw new IOException("No version found in package.json");
    } else {
      throw new IOException("No package.json - not an npm project.");
    }
  }

  public void setProjectVersion(String newVersion) throws IOException {

    final File PACKAGE_JSON = new File(getToolPath().toString() + "node_modules/npm/package.json");
    if (PACKAGE_JSON.isFile()) {
      File backupFile = new File("package.json.bak");

      if (!PACKAGE_JSON.renameTo(backupFile)) {
        throw new IOException("Failed to create backup of package.json");
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(backupFile));
          PrintWriter writer = new PrintWriter(new FileWriter(PACKAGE_JSON))) {
        String line;
        while ((line = reader.readLine()) != null) {
          // Replace version if line contains "version"
          if (line.contains("\"version\"")) {
            line = line.replaceAll("\"version\"\\s*:\\s*\"[^\"]*\"", "\"version\": \"" + newVersion + "\"");
          }
          writer.println(line);
        }
      }

      // Delete backup file
      if (!backupFile.delete()) {
        throw new IOException("Failed to delete backup file");
      }
    } else {
      throw new IOException("No package.json - not an npm project.");
    }
  }

  public void checkTopLevelProject() throws IOException {

    final File PACKAGE_JSON = new File(getToolPath().toString() + "node_modules/npm/package.json");
    if (!PACKAGE_JSON.isFile()) {
      throw new IOException("No package.json - not an npm project.");
    }
    // IMHO npm/package.json does not support nested projects (modules)
  }
}
