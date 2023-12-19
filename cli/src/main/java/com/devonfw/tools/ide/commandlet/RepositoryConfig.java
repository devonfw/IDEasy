package com.devonfw.tools.ide.commandlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * Represents the configuration of a repository to be used by the {@link RepositoryCommandlet}.
 *
 * @param path Path into which the project is cloned. This path is relative to the workspace.
 * @param workingSets The working sets associated with the repository.
 * @param workspace Workspace to use for checkout and import. Default is main.
 * @param gitUrl Git URL to use for cloning the project.
 * @param gitBranch Git branch to checkout. Git default branch is default.
 * @param buildPath The build path for the repository.
 * @param buildCmd The command to invoke to build the repository after clone or pull. If omitted no build is triggered.
 * @param imports list of IDEs where the repository will be imported to.
 * @param active {@code true} to setup the repository during setup, {@code false} to skip.
 */
public record RepositoryConfig(
    String path,
    String workingSets,
    String workspace,
    String gitUrl,
    String gitBranch,
    String buildPath,
    String buildCmd,
    Set<String> imports,
    boolean active) {
  public static RepositoryConfig loadProperties(Path filePath) {

    Properties properties = new Properties();
    try (InputStream input = new FileInputStream(filePath.toString())) {
      properties.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + filePath, e);
    }

    Set<String> importsSet = getImports(properties);

    return new RepositoryConfig(properties.getProperty("path"), properties.getProperty("workingsets"),
        properties.getProperty("workspace"), properties.getProperty("git_url"), properties.getProperty("git_branch"),
        properties.getProperty(("build_path")), properties.getProperty("build_cmd"), importsSet,
        Boolean.parseBoolean(properties.getProperty("active")));
  }

  private static Set<String> getImports(Properties properties) {

    String importProperty = properties.getProperty("import");
    if (importProperty != null && !importProperty.isEmpty()) {
      return Set.of(importProperty.split("\\s*,\\s*"));
    }

    String legacyImportProperty = properties.getProperty("eclipse");
    if ("import".equals(legacyImportProperty)) {
      return Set.of("eclipse");
    } else {
      return Collections.emptySet();
    }
  }
}
