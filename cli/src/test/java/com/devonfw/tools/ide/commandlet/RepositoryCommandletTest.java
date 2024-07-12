package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

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
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    rc.repository.setValueAsString(PROPERTIES_FILE, this.context);
    // act
    rc.run();
    // assert
    assertLogMessage(this.context, IdeLogLevel.INFO, "Importing repository from " + PROPERTIES_FILE + " ...");
  }

  @Test
  public void testRunWithNoSpecificRepositoryAndInactive() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    // act
    rc.run();
    // assert
    assertLogMessage(this.context, IdeLogLevel.INFO, "Importing repository from " + PROPERTIES_FILE + " ...");
    assertLogMessage(this.context, IdeLogLevel.INFO, "Skipping repository - use force (-f) to setup all repositories ...");
  }

  @Test
  public void testRunInvalidConfiguration() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    createPropertiesFile();
    this.properties.setProperty("path", "");
    this.properties.setProperty("git_url", "");
    saveProperties(this.properties);
    rc.repository.setValueAsString(PROPERTIES_FILE, this.context);
    // act
    rc.run();
    // assert
    assertLogMessage(this.context, IdeLogLevel.WARNING,
        "Invalid repository configuration " + PROPERTIES_FILE + " - both 'path' and 'git-url' have to be defined.");
  }

  @Test
  public void testRunNoRepositoriesOrProjectsFolderFound() {

    // arrange
    RepositoryCommandlet rc = this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    Path repositoriesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
    this.context.getFileAccess().delete(repositoriesPath);
    // act
    rc.run();
    // assert
    assertLogMessage(this.context, IdeLogLevel.WARNING, "Cannot find repositories folder nor projects folder.");
  }

  private void createPropertiesFile() {

    try {
      this.properties.setProperty("path", "test");
      this.properties.setProperty("workingsets", "test");
      this.properties.setProperty("workspace", "test");
      this.properties.setProperty("git_url", "test");
      this.properties.setProperty("git_branch", "test");
      this.properties.setProperty("build_path", "test");
      this.properties.setProperty("build_cmd", "");
      this.properties.setProperty("active", "false");

      Path propertiesPath = this.context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES)
          .resolve(PROPERTIES_FILE);
      this.context.getFileAccess().mkdirs(propertiesPath.getParent());
      Files.createFile(propertiesPath);
      saveProperties(this.properties);
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