package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RepositoryCommandletTest extends AbstractIdeContextTest {

  IdeTestContext context = newContext(PROJECT_BASIC);

  private static final String PROPERTIES_FILE = "test.properties";

  /**
   * Properties object used to write key-value pairs to the properties file "test.properties"
   */
  Properties properties = new Properties();

  @Test
  public void testRunWithSpecificRepository() {

    // arrange
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    rc.repository.setValueAsString(PROPERTIES_FILE, context);
    // act
    rc.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "Importing repository from " + PROPERTIES_FILE + " ...");
  }

  @Test
  public void testRunWithNoSpecificRepositoryAndInactive() {

    // arrange
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    // act
    rc.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "Importing repository from " + PROPERTIES_FILE + " ...");
    assertLogMessage(context, IdeLogLevel.INFO, "Skipping repository - use force (-f) to setup all repositories ...");
  }

  @Test
  public void testRunInvalidConfiguration() {

    // arrange
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    properties.setProperty("path", "");
    properties.setProperty("git_url", "");
    saveProperties(properties);
    rc.repository.setValueAsString(PROPERTIES_FILE, context);
    // act
    rc.run();
    // assert
    assertLogMessage(context, IdeLogLevel.WARNING,
        "Invalid repository configuration " + PROPERTIES_FILE + " - both 'path' and 'git-url' have to be defined.");
  }

  @Test
  public void testRunNoRepositoriesOrProjectsFolderFound() {

    // arrange
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    Path repositoriesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
    this.context.getFileAccess().delete(repositoriesPath);
    // act
    rc.run();
    // assert
    assertLogMessage(context, IdeLogLevel.WARNING, "Cannot find repositories folder nor projects folder.");
  }

  private void createPropertiesFile() {

    try {
      properties.setProperty("path", "test");
      properties.setProperty("workingsets", "test");
      properties.setProperty("workspace", "test");
      properties.setProperty("git_url", "test");
      properties.setProperty("git_branch", "test");
      properties.setProperty("build_path", "test");
      properties.setProperty("build_cmd", "");
      properties.setProperty("active", "false");

      Path propertiesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES)
          .resolve(PROPERTIES_FILE);
      this.context.getFileAccess().mkdirs(propertiesPath.getParent());
      Files.createFile(propertiesPath);
      saveProperties(properties);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create properties file during tests.", e);
    }

  }

  private void saveProperties(Properties properties) {

    Path propertiesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES)
        .resolve(PROPERTIES_FILE);
    try (var output = Files.newOutputStream(propertiesPath)) {
      properties.store(output, null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save properties file during tests.", e);
    }
  }

}