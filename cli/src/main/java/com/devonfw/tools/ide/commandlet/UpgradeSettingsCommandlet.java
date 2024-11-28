package com.devonfw.tools.ide.commandlet;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/**
 * {@link Commandlet} to upgrade settings after a migration from devofw-ide to IDEasy
 */
public class UpgradeSettingsCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */

  public UpgradeSettingsCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "upgrade-settings";
  }

  @Override
  public void run() {
    updateDevonProperties();
    replaceLegacyVariablesAndBracketsInWorkspace();
    checkIfLegacyFolderExists();
    handleReplacementPatternsFiles();
    checkForXMLNameSpace();
  }

  private void checkIfLegacyFolderExists() {
    this.context.info("Scanning for legacy folders...");

    Path devonFolder = context.getIdeHome().resolve("settings/devon");

    Path templatesFolder = context.getIdeHome().resolve("settings/templates");

    Path projectsFolder = context.getIdeHome().resolve("settings/projects");

    Path repositoriesFolder = context.getIdeHome().resolve("settings/repositories");

    if (Files.exists(devonFolder) && Files.isDirectory(devonFolder)) {
      try {
        if (!Files.exists(templatesFolder)) {
          Files.move(devonFolder, templatesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/devon' to 'settings/templates'.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/devon' folder to 'settings/templates': " + e.getMessage());
      }
    } else {
      this.context.warning("The 'templates' folder already exists, skipping renaming.");
    }
    if (Files.exists(projectsFolder) && Files.isDirectory(projectsFolder)) {
      try {
        if (!Files.exists(repositoriesFolder)) {
          Files.move(projectsFolder, repositoriesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/projects' to 'settings/repositories'.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/projects' folder to 'settings/repositories': " + e.getMessage());
      }
    } else {
      this.context.warning("The 'repositories' folder already exists, skipping renaming.");
    }
  }

  private void replaceLegacyVariablesAndBracketsInWorkspace() {
    this.context.info("Scanning for legacy variables...");

    Map<String, String> legacyToNewMapping = Map.of(
        "${DEVON_IDE_HOME}", "$[IDE_HOME]",
        "${MAVEN_VERSION}", "$[MVN_VERSION]",
        "${SETTINGS_PATH}", "$[IDE_HOME]/settings"
    );

    Path settingsDirectory = context.getIdeHome().resolve("settings");

    try {
      Files.walk(settingsDirectory)
          .filter(path -> Files.isDirectory(path) && path.getFileName().toString().equals("workspace"))
          .forEach(workspaceDir -> {
            try {
              Files.walk(workspaceDir)
                  .filter(Files::isRegularFile)
                  .forEach(file -> {
                    try {
                      String content = Files.readString(file);
                      String originalContent = content;

                      for (Map.Entry<String, String> entry : legacyToNewMapping.entrySet()) {
                        content = content.replace(entry.getKey(), entry.getValue());
                      }

                      content = content.replace("{", "[");
                      content = content.replace("}", "]");

                      if (!content.equals(originalContent)) {
                        Files.writeString(file, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                        this.context.success("Successfully updated variables/brackets " + file);
                      }
                    } catch (AccessDeniedException e) {
                      this.context.error("Access denied to file: " + file + ", exception: " + e);
                    } catch (IOException e) {
                      this.context.error("Error processing file: " + file + ", exception: " + e);
                    }
                  });
            } catch (IOException e) {
              this.context.error("Error while processing files in workspace: " + workspaceDir, e);
            }
          });
    } catch (IOException e) {
      this.context.error("Error while scanning for workspace directories", e);
    }
  }

  private void handleReplacementPatternsFiles() {
    this.context.info("Scanning for legacy files");

    Path settingsDirectory = context.getIdeHome().resolve("settings");

    try {
      Files.walk(settingsDirectory)
          .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().equals("replacement-patterns.properties"))
          .forEach(file -> {
            try {
              String content = Files.readString(file);
              if (!content.trim().isEmpty()) {
                this.context.warning("The file 'replacement-patterns.properties' is not empty: " + file);
              }

              Files.delete(file);
              this.context.success("Deleted 'replacement-patterns.properties' from: " + file);
            } catch (IOException e) {
              this.context.error("Error processing 'replacement-patterns.properties' file: " + file, e);
            }
          });
    } catch (IOException e) {
      this.context.error("Error scanning for 'replacement-patterns.properties' file", e);
    }
  }


  private void checkForXMLNameSpace() {
    this.context.info("Scanning XML files...");
    Path settingsDirectory = context.getIdeHome().resolve("settings");
    AtomicBoolean missingNamespaceFound = new AtomicBoolean(false);
    try {
      Files.walk(settingsDirectory)
          .filter(path -> Files.isDirectory(path) && path.getFileName().toString().equals("workspace"))
          .forEach(workspaceDir -> {
            try {
              Files.walk(workspaceDir)
                  .filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".xml"))
                  .forEach(file -> {
                    try {
                      String content = Files.readString(file);

                      if (!content.contains("xmlns:merge=\"https://github.com/devonfw/IDEasy/merge\"")) {
                        this.context.warning("The XML file " + file + " does not contain the required 'xmlns:merge' attribute.");
                        missingNamespaceFound.set(true);
                      }
                    } catch (IOException e) {
                      this.context.error("Error reading the file: " + file, e);
                    }
                  });
            } catch (IOException e) {
              this.context.error("Error processing the workspace: " + workspaceDir, e);
            }
          });
      if (missingNamespaceFound.get()) {
        this.context.warning("For further information, please visit https://github.com/devonfw/IDEasy/blob/main/documentation/configurator.adoc#xml-merger");
      } else {
        this.context.success("Your XML files are up to date");
      }

    } catch (IOException e) {
      this.context.error("Error walking through the 'settings' directory", e);
    }
  }

  private void createCustomToolsJson(String variable) {
    try {
      JSONArray tabelObject = new JSONArray();
      variable = variable.substring(variable.indexOf("(") + 1, variable.indexOf(")") - 1);
      String url = variable.substring(variable.indexOf("https://"), variable.indexOf(" "));
      String[] toolsArray = variable.split(" ");
      for (String tool : toolsArray) {
        JSONObject toolObject = new JSONObject();
        String[] toolParts = tool.split(":");
        toolObject.put("name", toolParts[0]);
        toolObject.put("version", toolParts[1]);
        if (toolParts.length > 2) {
          toolObject.put("os-agnostic", toolParts[2].equals("all"));
          toolObject.put("arch-agnostic", toolParts[2].equals("all"));
        } else {
          toolObject.put("os-agnostic", false);
          toolObject.put("arch-agnostic", false);
        }
        tabelObject.add(toolObject);
      }
      JSONObject finalObject = new JSONObject();
      finalObject.put("tools", tabelObject);
      finalObject.put("url", url);

      FileWriter writer = new FileWriter(context.getIdeHome().resolve("settings/custom-tools.json").toString());
      writer.write(finalObject.toJSONString());

      writer.flush();
      writer.close();

      context.success("Created custom-tools.json at: " + context.getIdeHome().resolve("settings/custom-tools.json"));
    } catch (IOException e) {
      throw new RuntimeException(e);

    }
  }

  private void updateDevonProperties() {
    List<Path> pathList = new ArrayList<>();
    EnvironmentVariables devonPropertiesPath = context.getVariables();
    while (devonPropertiesPath != null) {
      if (devonPropertiesPath.getPropertiesFilePath() != null) {
        pathList.add(devonPropertiesPath.getPropertiesFilePath().getParent().resolve("devon.properties"));
      }
      devonPropertiesPath = devonPropertiesPath.getParent();
    }

    for (Path file_path : pathList) {
      if (!Files.exists(file_path)) {
        continue;
      }
      Path target = file_path.getParent().resolve("ide.properties");
      Properties devonProperties = new Properties();
      devonProperties.put("IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED", "false");
      try {
        List<String> readLines = Files.readAllLines(file_path);
        String[] split;
        for (String line : readLines) {
          if (!line.contains("#") && !line.isEmpty()) {
            if (line.contains("DEVON_IDE_CUSTOM_TOOLS")) {
              createCustomToolsJson(line);
              continue;
            }
            if (line.contains("DEVON_")) {
              line = line.replace("DEVON_", "");
            }
            split = line.split("[ =]");
            if (split.length == 3) {
              devonProperties.put(split[1], new String[] { split[0], split[2] });
            }
            if (split.length == 2) {
              devonProperties.put(split[0], split[1]);
            }
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (context.getFileAccess().findFirst(file_path.getParent(), path -> path.getFileName().toString().equals("ide.properties"), false) != null) {
        try {
          List<String> readLines = Files.readAllLines(target);
          String[] split;

          Properties ideProperties = new Properties();
          for (String line : readLines) {
            if (!line.contains("#") && !line.isEmpty()) {
              split = line.split("[ =]");
              if (split.length == 3) {
                ideProperties.put(split[1], new String[] { split[0], split[2] });
              }
              if (split.length == 2) {
                ideProperties.put(split[0], split[1]);
              }
            }
          }

          Properties mergedProperties = new Properties();
          mergedProperties.putAll(ideProperties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue())));
          mergedProperties.putAll(devonProperties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue())));
          for (Entry<Object, Object> set : ideProperties.entrySet()) {
            mergedProperties.remove(set.getKey());
          }

          for (Entry<Object, Object> set : mergedProperties.entrySet()) {
            if (set.getValue() instanceof String) {
              Files.write(target, ("\n" + set.getKey().toString() + "=" + set.getValue().toString()).getBytes(), StandardOpenOption.APPEND);
            }
            if (set.getValue() instanceof String[]) {

              String[] values = (String[]) set.getValue();
              Files.write(target, ("\n" + values[0] + " " + set.getKey().toString() + "=" + values[1]).getBytes(), StandardOpenOption.APPEND);
            }
          }

          this.context.success("Successfully merged and updated ide.properties: " + file_path);

          Files.delete(file_path);

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        try {
          String comment = "#********************************************************************************\n"
              + "# This file contains user specific environment variables.\n"
              + "# You may reconfigure some variables to tweak devonfw-ide to your personal needs.\n"
              + "# However, you do so at your own risk!!!\n"
              + "# Only change variables if you know exactly what you are doing!!!\n"
              + "# For details see:\n"
              + "# https://github.com/devonfw/ide/blob/master/documentation/configuration.asciidoc#configuration\n"
              + "#********************************************************************************\n"
              + "\n"
              + "# Uncomment the following line to use your shared maven repository.\n"
              + "# This will save diskspace but comes with the risk that other projects could\n"
              + "# accidentally (or due to an security attack) access and use artifacts from this project.\n"
              + "# If you are working in a project that is highly sensitive never use this feature.\n"
              + "#export M2_REPO=~/.m2/repository\n"
              + "\n"
              + "# In case you are sitting behind a proxy these JVM options may help:\n"
              + "#export JAVA_OPTS=-Dhttp.proxyHost=myproxy.com -Dhttp.proxyPort=8080\n";
          Files.write(file_path, comment.getBytes());
          for (Entry<Object, Object> set : devonProperties.entrySet()) {
            if (set.getValue() instanceof String) {
              Files.write(file_path, ("\n" + set.getKey().toString() + "=" + set.getValue().toString()).getBytes(), StandardOpenOption.APPEND);
            }
            if (set.getValue() instanceof String[]) {
              String[] values = (String[]) set.getValue();
              Files.write(file_path, ("\n" + values[0] + " " + set.getKey().toString() + "=" + values[1]).getBytes(), StandardOpenOption.APPEND);
            }
          }
          Files.move(file_path, target);
          this.context.success("Updated file name: " + file_path + "\n-> " + target + "and updated variables");
        } catch (IOException e) {
          this.context.error("Error updating file name: " + file_path, e);
        }
      }
    }
  }
}
