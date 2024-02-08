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
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="https://nodejs.org/">node</a>.
 */

public class Npm extends LocalToolCommandlet {

  final String JSON_PATH = "/node/node_modules/npm/package.json";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.RUNTIME));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void run() {

    try {
      runNpm();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  public void runNpm() throws IOException {

    String[] userInputArray = this.arguments.asArray();
    // TODO
    String npmBuildOpts = "buildOpts";
    String npmReleaseOpts = "releaseOpts";

    if (userInputArray.length > 0) {
      String firstElement = userInputArray[0].toString();
      if (firstElement.equals("h") || firstElement.equals("help")) {
        System.out.println("Setup or run npm.");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("                                run default build");
        System.out.println(" setup                          setup NPM (install and verify)");
        System.out.println(" get-version                    get the current project version");
        System.out.println(
            " set-version «nv» [«cv»]        set the current project version to new version «nv» (assuming current version is «cv»)");
        System.out.println(
            " check-top-level-project        check if you are running on a top-level project or fail if in a module or no NPM project at all");
        System.out.println(" release                        start a clean deploy release build");
        System.out.println(" «args»                         run NPM with the given arguments");
        return;
      }

      if (userInputArray.length >= 2) {
        String secondElement = userInputArray[1];

        if (firstElement == "shortlist" && (secondElement == null || secondElement.length() == 0)) {
          this.context.info("setup version get-version set-version check-top-level-project release help");
        }

        if ((firstElement == null || firstElement.length() == 0) || firstElement.equals("build")) {
          if (npmBuildOpts.length() != 0) {
            // runTool(getConfiguredVersion()); // (NPM_BUILD_OPTS);
          } else {
            // run();
          }

          if (secondElement.length() != 0) {
            // runTool(getConfiguredVersion());// (restliche Argumente)
          }
        } else if (secondElement.equals("setup")) {
          // doSetup(thirdElement);
        } else if (secondElement.equals("get-version")) {
          this.context.info("Project version: {}", getProjectVersion());
        } else if (secondElement.equals("set-version")
            && (userInputArray.length >= 3 ? userInputArray[2].length() != 0 : false)) {
          setProjectVersion(userInputArray[2]);
          this.context.info("Project version is set to {}", userInputArray[2]);
        } else if (secondElement.equals("check-top-level-project")) {
          checkTopLevelProject();
        } else if (secondElement.equals("release")) {
          // runTool(getConfiguredVersion());// (npmReleaseOpts != null ? npmReleaseOpts : "release")
        } else {
          // runTool(getConfiguredVersion()); // (alle übergegebene Argumente)
        }
      }
    }
  }

  public void doRunBuild(boolean silent, String[] arguments) {

    this.context.info("Running: npm {}", arguments);
    install(silent);
  }

  public void doBuild(String scriptName) {

    // doRunBuild install
    if (isPackageJsonContainingScript(scriptName)) {
      // doRunBuild run build
    }
  }

  public boolean isPackageJsonContainingScript(String scriptName) {

    this.context.trace("Überprüfung, ob package.json einen Skriptabschnitt namens " + scriptName + " enthält");
    try {
      BufferedReader reader = new BufferedReader(
          new FileReader(createFileWithPath(this.context.getSoftwarePath().toString() + JSON_PATH)));
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append(System.lineSeparator());
      }
      reader.close();
      String jsonString = content.toString().replaceAll("\\r", "");
      Pattern pattern = Pattern.compile("[\"']scripts[\"']\\s*:\\s*\\{\\s*§.*[\"']" + scriptName + "[\"']\\s*:");
      Matcher matcher = pattern.matcher(jsonString);
      if (matcher.find()) {
        return true;
      } else {
        this.context
            .info("Im package.json ist kein Build-Skript vorhanden - überspringe Ausführung des Build-Skripts.");
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void doSetup() {

    String TOOL_PATH = this.context.getSoftwarePath().toString() + "/node";
    String softwareDir = TOOL_PATH + "/node_modules/npm/bin";
    // boolean successfullInstallation
    install();
    if (createFileWithPath(TOOL_PATH + "/npm").isFile()) {
      deleteFile(TOOL_PATH + "/npm");
    }

    if (createFileWithPath(TOOL_PATH + "/npm.cmd").isFile()) {
      deleteFile(TOOL_PATH + "/npm.cmd");
    }

    if (createFileWithPath(TOOL_PATH + "/npx").isFile()) {
      deleteFile(TOOL_PATH + "/npx");
    }

    if (createFileWithPath(TOOL_PATH + "/npx.cmd").isFile()) {
      deleteFile(TOOL_PATH + "/npx.cmd");
    }

    copyFile(softwareDir + "/npm", TOOL_PATH);
    copyFile(softwareDir + "/npm.cmd", TOOL_PATH);
    copyFile(softwareDir + "/npx", TOOL_PATH);
    copyFile(softwareDir + "/npx.cmd", TOOL_PATH);
  }

  private File createFileWithPath(String path) {

    return new File(path);
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

    File PACKAGE_JSON = createFileWithPath(this.context.getSoftwarePath().toString() + JSON_PATH);
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

    File PACKAGE_JSON = createFileWithPath(this.context.getSoftwarePath().toString() + JSON_PATH);
    if (PACKAGE_JSON.isFile()) {
      File backupFile = new File("package.json.bak");

      if (!PACKAGE_JSON.renameTo(backupFile)) {
        this.context.error("Failed to create backup of package.json");
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
        this.context.error("Failed to delete backup file");
      }
    } else {
      this.context.error("No package.json - not an npm project.");
    }
  }

  public void checkTopLevelProject() {

    File PACKAGE_JSON = createFileWithPath(this.context.getSoftwarePath().toString() + JSON_PATH);
    if (!PACKAGE_JSON.isFile()) {
      this.context.error("No package.json - not an npm project.");
    }
    // IMHO npm/package.json does not support nested projects (modules)
  }
}
