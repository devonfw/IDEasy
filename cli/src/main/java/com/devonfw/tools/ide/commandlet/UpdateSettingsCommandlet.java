package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.context.IdeContext;

public class UpdateSettingsCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */

  private Map<String, String> legacyToNew;

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
    CheckIfLegacyFolderExists();
    Path source = context.getIdeHome();
    List<Path> test = context.getFileAccess().listChildrenRecursive(source, path -> path.getFileName().toString().equals("devon.properties"));
    for (Path file_path : test) {

      Path target = file_path.getParent().resolve("ide.properties");
      Properties devonProperties = new Properties();
      try (InputStream devonInputStream = Files.newInputStream(file_path)) {
        devonProperties.load(devonInputStream);

        for (String name : devonProperties.stringPropertyNames()) {
          if (name.contains("DEVON_")) {
            devonProperties.put(name.replace("DEVON_", ""), devonProperties.get(name));
            devonProperties.remove(name);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (context.getFileAccess().findFirst(file_path.getParent(), path -> path.getFileName().toString().equals("ide.properties"), false) != null) {
        try {
          Properties ideProperties = new Properties();
          try (InputStream ideInputStream = Files.newInputStream(target)) {
            ideProperties.load(ideInputStream);
          }

          Properties mergedProperties = new Properties();
          mergedProperties.putAll(ideProperties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
          mergedProperties.putAll(devonProperties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
          for (String name : ideProperties.stringPropertyNames()) {
            mergedProperties.remove(name);
          }

          Files.write(target, mergedProperties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList()),
              StandardOpenOption.APPEND);

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
          Files.write(file_path, devonProperties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList()),
              StandardOpenOption.APPEND);
          Files.move(file_path, target);
          this.context.success("Updated file name: " + file_path + "\n-> " + target);
        } catch (IOException e) {
          this.context.error("Error updating file name: " + file_path, e);
        }
      }
    }
  }

  public void CheckIfLegacyFolderExists() {
    // Path to the "devon" folder
    Path devonFolder = context.getIdeHome().resolve("settings/devon");
    // Path to the new "templates" folder
    Path templatesFolder = context.getIdeHome().resolve("settings/templates");

    // Path to the "projects" folder
    Path projectsFolder = context.getIdeHome().resolve("settings/projects");
    // Path to the "repositories" folder
    Path repositoriesFolder = context.getIdeHome().resolve("settings/repositories");

    // Check if the "devon" folder exists
    if (Files.exists(devonFolder) && Files.isDirectory(devonFolder)) {
      try {
        // Check if the "templates" folder already exists to avoid collisions
        if (!Files.exists(templatesFolder)) {
          // Rename the "devon" folder to "templates"
          Files.move(devonFolder, templatesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/devon' to 'settings/templates'.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/devon' folder to 'settings/templates': " + e.getMessage());
      }
    } else {
      this.context.warning("The 'templates' folder already exists, skipping renaming.");
    }
    // Check if the "projects" folder already exists
    if (Files.exists(projectsFolder) && Files.isDirectory(projectsFolder)) {
      try {
        // Check if the "repositories" folder already exists to avoid collisions
        if (!Files.exists(repositoriesFolder)) {
          // Rename the "projects" folder to "repositories"
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
}
