package com.devonfw.tools.ide.git.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Properties} for {@link RepositoryConfig}.
 */
final class RepositoryProperties {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryProperties.class);

  private static final String PROPERTY_PATH = "path";
  private static final String PROPERTY_WORKING_SETS = "workingsets";
  private static final String PROPERTY_WORKSPACES = "workspaces";
  private static final String PROPERTY_GIT_URL = "git_url";
  private static final String PROPERTY_BUILD_PATH = "build_path";
  private static final String PROPERTY_BUILD_CMD = "build_cmd";
  private static final String PROPERTY_ACTIVE = "active";
  private static final String PROPERTY_GIT_BRANCH = "git_branch";
  private static final String PROPERTY_IMPORT = "import";
  private static final String PROPERTY_LINK = "link";
  private static final String PROPERTY_LINK_TARGET = "link (=<target>)";
  private static final String PROPERTY_ECLIPSE = "eclipse";

  private static final Pattern PATH_PATTERN = Pattern.compile("[a-zA-Z0-9_.$/-]+");

  private static final Pattern WORKSPACE_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.-]+");

  private final Path file;

  private final Properties properties;

  private boolean invalid;

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

    return getProperty(name, null, required);
  }

  /**
   * @param name the name of the requested property.
   * @param legacyName the optional legacy property name.
   * @param required - {@code true} if the requested property is required, {@code false} otherwise.
   * @return the value of the requested property or {@code null} if undefined.
   */
  public String getProperty(String name, String legacyName, boolean required) {

    String value = doGetProperty(name, legacyName);
    if (isEmpty(value)) {
      if (required) {
        LOG.error("The properties file {} is invalid because the required property {} is not present. Ignoring this file.", this.file, name);
        this.invalid = true;
      }
      return null;
    }
    return value;
  }

  private String doGetProperty(String name, String legacyName) {

    String value = this.properties.getProperty(name);
    if (value != null) {
      return value;
    }
    if (legacyName != null) {
      value = getLegacyProperty(legacyName, name);
      if (value != null) {
        return value;
      }
    }
    legacyName = name.replace("_", ".");
    if (!legacyName.equals(name)) {
      value = getLegacyProperty(legacyName, name);
      if (value != null) {
        return value;
      }
      legacyName = name.replace("_", "-");
      value = getLegacyProperty(legacyName, name);
    }
    return value;
  }

  private static boolean isEmpty(String value) {

    return (value == null) || value.isBlank();
  }

  private String getLegacyProperty(String legacyName, String name) {

    String value = this.properties.getProperty(legacyName);
    if (value != null) {
      LOG.warn("In the properties file {} please replace the legacy property {} with the official property {}", this.file, legacyName, name);
    }
    return value;
  }

  /**
   * @return {@code true} if these properties have been marked as invalid because a required property was requested that is not available, {@code false}
   *     otherwise.
   */
  public boolean isInvalid() {

    return this.invalid;
  }

  /**
   * @return the {@link RepositoryConfig#path() path}.
   */
  public String getPath() {

    return sanatizeRelativePath(getProperty(PROPERTY_PATH), PROPERTY_PATH);
  }

  /**
   * @return the {@link RepositoryConfig#workingSets() working sets}.
   */
  public String getWorkingSets() {

    return getProperty(PROPERTY_WORKING_SETS);
  }

  /**
   * @return the {@link RepositoryConfig#gitUrl()}  git url}.
   */
  public String getGitUrl() {

    return getProperty(PROPERTY_GIT_URL, true);
  }

  /**
   * @return the {@link RepositoryConfig#gitBranch()}  git branch}.
   */
  public String getGitBranch() {

    return getProperty(PROPERTY_GIT_BRANCH);
  }

  /**
   * @return the {@link RepositoryConfig#buildPath() build path}.
   */
  public String getBuildPath() {

    return getProperty(PROPERTY_BUILD_PATH);
  }

  /**
   * @return the {@link RepositoryConfig#buildCmd() build command}.
   */
  public String getBuildCmd() {

    return getProperty(PROPERTY_BUILD_CMD);
  }

  /**
   * @return the {@link RepositoryConfig#active() active flag}.
   */
  public boolean isActive() {

    return parseBoolean(getProperty(PROPERTY_ACTIVE));
  }

  /**
   * @return the IDEs where to import the repository.
   */
  public Set<String> getImports() {

    String importProperty = getProperty(PROPERTY_IMPORT);
    if (importProperty != null) {
      if (importProperty.isEmpty()) {
        return Set.of();
      }
      return Arrays.stream(importProperty.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
    }

    String legacyImportProperty = getLegacyProperty(PROPERTY_ECLIPSE, PROPERTY_IMPORT);
    if ("import".equals(legacyImportProperty)) {
      LOG.warn("Property {} is deprecated and should be replaced with {} (invert key and value).", PROPERTY_ECLIPSE,
          PROPERTY_IMPORT);
      return Set.of("eclipse");
    } else {
      return Set.of();
    }
  }

  /**
   * @return the workspaces where to clone the repository. Returns a set containing "main" as default if not specified.
   */
  public List<String> getWorkspaces() {

    String workspaceProperty = getProperty(PROPERTY_WORKSPACES, "workspace", false);
    if (!isEmpty(workspaceProperty)) {
      if (RepositoryConfig.WORKSPACE_NAME_ALL.equals(workspaceProperty)) {
        return List.of(workspaceProperty);
      }
      List<String> list = new ArrayList<>();
      Set<String> set = new HashSet<>();
      for (String workspace : workspaceProperty.split(",")) {
        workspace = workspace.trim();
        if (WORKSPACE_PATTERN.matcher(workspace).matches()) {
          boolean added = set.add(workspace);
          if (added) {
            list.add(workspace);
          } else {
            LOG.warn("Ignoring duplicate workspace {} from {}", workspace, workspaceProperty);
          }
        } else {
          LOG.warn("Ignoring illegal workspace {} from {}", workspace, workspaceProperty);
        }
      }
      return Collections.unmodifiableList(list);
    }
    return List.of(IdeContext.WORKSPACE_MAIN);
  }

  public List<RepositoryLink> getLinks() {

    String link = getProperty(PROPERTY_LINK);
    if (isEmpty(link)) {
      return List.of();
    }
    List<RepositoryLink> links = new ArrayList<>();
    for (String linkItem : link.split(",")) {
      String linkPath = linkItem;
      String linkTarget = "";
      int eqIndex = linkItem.indexOf('=');
      if (eqIndex > 0) {
        linkPath = linkItem.substring(0, eqIndex);
        linkTarget = linkItem.substring(eqIndex + 1);
      }
      linkPath = sanatizeRelativePath(linkPath, PROPERTY_LINK);
      if (!linkTarget.isEmpty()) {
        linkTarget = sanatizeRelativePath(linkTarget, PROPERTY_LINK_TARGET);
      }
      if ((linkPath != null) && (linkTarget != null)) {
        links.add(new RepositoryLink(linkPath, linkTarget));
      }
    }
    return List.copyOf(links); // make immutable for record
  }

  private String sanatizeRelativePath(String path, String propertyName) {
    if (path == null) {
      return null;
    }
    String normalized = path.trim().replace('\\', '/');
    if (normalized.contains("..") || !PATH_PATTERN.matcher(normalized).matches()) {
      LOG.warn("Invalid path {} from property {} of {}", path, propertyName, this.file);
      return null;
    }
    return normalized;
  }

  private static boolean parseBoolean(String value) {

    if (value == null) {
      return true;
    }
    return "true".equals(value.trim());
  }
}
