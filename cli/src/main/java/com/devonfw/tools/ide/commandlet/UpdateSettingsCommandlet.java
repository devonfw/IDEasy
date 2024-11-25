package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
    CheckIfLegacyFolderExists();
    Path source = context.getIdeHome();
    List<Path> test = context.getFileAccess().listChildrenRecursive(source, path -> path.getFileName().toString().equals("devon.properties"));
    for (Path file_path : test) {

      if (context.getFileAccess().findFirst(file_path.getParent(), path -> path.getFileName().toString().equals("ide.properties"), false) != null) {
        try {
          Path target = file_path.getParent().resolve("ide.properties");

          // Load the devon.properties file (only properties, no comment merging)
          Properties devonProperties = new Properties();
          try (InputStream devonInputStream = Files.newInputStream(file_path)) {
            devonProperties.load(devonInputStream); // Load properties from devon.properties
          }

          // Load the ide.properties file and preserve its comments
          Properties ideProperties = new Properties();
          List<String> ideLines = Files.readAllLines(target, StandardCharsets.UTF_8);
          List<String> comments = new ArrayList<>();

          // Separate comments from the actual properties in ide.properties
          int i = 0;
          while (i < ideLines.size() && ideLines.get(i).startsWith("#") && ideLines.get(i).startsWith(" ")) {
            comments.add(ideLines.get(i));  // Add comment lines to the list
            i++;
          }

          // Now load the properties from ide.properties
          try (InputStream ideInputStream = Files.newInputStream(target)) {
            ideProperties.load(ideInputStream); // Load properties from ide.properties
          }

          // Merge the properties from devon.properties into ide.properties
          for (String key : devonProperties.stringPropertyNames()) {
            // Overwrite existing properties or add new ones from devon.properties
            ideProperties.setProperty(key, devonProperties.getProperty(key));
          }

          // Prepare the content to write back to ide.properties
          StringBuilder newContent = new StringBuilder();

          // Add all the comments from ide.properties to the top (preserving them)
          for (String comment : comments) {
            newContent.append(comment).append("\n");
          }

          // Write the merged properties (with no timestamp comment)
          for (String key : ideProperties.stringPropertyNames()) {
            newContent.append(key).append("=").append(ideProperties.getProperty(key)).append("\n");
          }

          // Write the new content back to ide.properties
          Files.writeString(target, newContent.toString());
          this.context.success("Successfully merged and updated ide.properties: " + file_path);

          // Delete the devon.properties file after merging
          Files.delete(file_path);

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        Path target = file_path.getParent().resolve("ide.properties");
        try {
          Files.move(file_path, target);
          this.context.success("Updated file name: " + file_path + "\n-> " + target);
        } catch (IOException e) {
          this.context.error("Error updating file name: " + file_path);
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
        } else {
          this.context.warning("The 'templates' folder already exists, skipping renaming.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/devon' folder to 'settings/templates': " + e.getMessage());
      }
    }
    // Check if the "projects" folder already exists
    if (Files.exists(projectsFolder) && Files.isDirectory(projectsFolder)) {
      try {
        // Check if the "repositories" folder already exists to avoid collisions
        if (!Files.exists(repositoriesFolder)) {
          // Rename the "projects" folder to "repositories"
          Files.move(projectsFolder, repositoriesFolder, StandardCopyOption.REPLACE_EXISTING);
          this.context.success("Successfully updated folder name from 'settings/projects' to 'settings/repositories'.");
        } else {
          this.context.warning("The 'repositories' folder already exists, skipping renaming.");
        }
      } catch (IOException e) {
        this.context.error("Error updating 'settings/projects' folder to 'settings/repositories': " + e.getMessage());
      }
    }
  }
}
