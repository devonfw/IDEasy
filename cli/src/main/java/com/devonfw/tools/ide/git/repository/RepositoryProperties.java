package com.devonfw.tools.ide.git.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Properties} for {@link RepositoryConfig}.
 */
final class RepositoryProperties {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryProperties.class);

  private final Path file;

  private final Properties properties;

  /**
   * The constructor.
   *
   * @param file the {@link Path} to the properties file.
   * @param context the {@link IdeContext}.
   */
  public RepositoryProperties(Path file, IdeContext context) {
    this(file, context.getFileAccess().readProperties(file));
  }

  /**
   * @param file the {@link Path} to the properties file.
   * @param properties the actual {@link Properties} loaded from the file.
   */
  RepositoryProperties(Path file, Properties properties) {
    super();
    this.file = file;
    this.properties = properties;
  }

  /**
   * @param name the name of the requested property.
   * @return the value of the requested property or {@code null} if undefined.
   */
  public String getProperty(String name) {
    return getProperty(name, false);
  }

  /**
   * @param name the name of the requested property.
   * @param required - {@code true} if the requested property is required, {@code false} otherwise.
   * @return the value of the requested property or {@code null} if undefined.
   */
  public String getProperty(String name, boolean required) {

    String value = this.properties.getProperty(name);
    if (value == null) {
      String legacyName = name.replace("_", ".");
      if (!legacyName.equals(name)) {
        value = getLegacyProperty(legacyName, name);
        if (value == null) {
          legacyName = name.replace("_", "-");
          value = getLegacyProperty(legacyName, name);
        }
      }
    }
    if (isEmpty(value)) {
      if (required) {
        throw new CliException("The properties file " + this.file + " must have a non-empty value for the required property " + name);
      }
      return null;
    }
    return value;
  }

  private static boolean isEmpty(String value) {

    return (value == null) || value.isBlank();
  }

  private String getLegacyProperty(String legacyName, String name) {

    String value = this.properties.getProperty(legacyName);
    if (value != null) {
      LOG.warn("The properties file {} uses the legacy property {} instead of {}", this.file, legacyName, name);
    }
    return value;
  }

  /**
   * @return the IDEs where to import the repository.
   */
  public Set<String> getImports() {

    String importProperty = this.properties.getProperty(RepositoryConfig.PROPERTY_IMPORT);
    if (importProperty != null) {
      if (importProperty.isEmpty()) {
        return Set.of();
      }
      return Arrays.stream(importProperty.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
    }

    String legacyImportProperty = getLegacyProperty(RepositoryConfig.PROPERTY_ECLIPSE, RepositoryConfig.PROPERTY_IMPORT);
    if ("import".equals(legacyImportProperty)) {
      LOG.warn("Property {} is deprecated and should be replaced with {} (invert key and value).", RepositoryConfig.PROPERTY_ECLIPSE,
          RepositoryConfig.PROPERTY_IMPORT);
      return Set.of("eclipse");
    } else {
      return Set.of();
    }
  }

  /**
   * @return the workspaces where to clone the repository. Returns a set containing "main" as default if not specified.
   */
  public List<String> getWorkspaces() {

    String workspaceProperty = this.properties.getProperty(RepositoryConfig.PROPERTY_WORKSPACES);
    if (workspaceProperty == null) {
      workspaceProperty = this.properties.getProperty("workspace");
      if (workspaceProperty != null) {
        LOG.debug("Property workspace is legacy, please change property name to workspaces in {}", this.file);
      }
    }
    if ((workspaceProperty != null) && !workspaceProperty.isEmpty()) {
      List<String> list = new ArrayList<>();
      Set<String> set = new HashSet<>();
      for (String workspace : workspaceProperty.split(",")) {
        workspace = workspace.trim();
        boolean added = set.add(workspace);
        if (added) {
          list.add(workspace);
        } else {
          LOG.warn("Ignoring duplicate workspace {} from {}", workspace, workspaceProperty);
        }
      }
      return Collections.unmodifiableList(list);
    }
    return List.of(IdeContext.WORKSPACE_MAIN);
  }

}
