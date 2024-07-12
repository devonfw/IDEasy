package com.devonfw.tools.ide.tool.ide;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Implementation of {@link PluginDescriptor}.
 */
public class PluginDescriptorImpl implements PluginDescriptor {

  private final String id;

  private final String name;

  private final String url;

  private final boolean active;

  private final Set<Tag> tags;

  /**
   * The constructor.
   *
   * @param id the {@link #getId() ID}.
   * @param name the {@link #getName() name}.
   * @param url the {@link #getUrl() URL}.
   * @param active the {@link #isActive() active flag}.
   * @param tags the {@link #getTags() tags}.
   */
  public PluginDescriptorImpl(String id, String name, String url, boolean active, Set<Tag> tags) {

    super();
    this.id = id;
    this.name = name;
    this.url = url;
    this.active = active;
    this.tags = tags;
  }

  @Override
  public String getId() {

    return this.id;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getUrl() {

    return this.url;
  }

  @Override
  public boolean isActive() {

    return this.active;
  }

  @Override
  public Set<Tag> getTags() {

    return this.tags;
  }

  /**
   * @param propertiesFile the {@link Path} to the plugin {@link Properties} file.
   * @param logger the {@link IdeLogger}.
   * @param needUrl - {@code true} if {@link PluginDescriptor#getUrl() URL} needs to be present and a warning shall be logged if missing, {@code false}
   * otherwise.
   * @return the loaded {@link PluginDescriptor}.
   */
  public static PluginDescriptor of(Path propertiesFile, IdeLogger logger, boolean needUrl) {

    Properties properties = new Properties();
    String name = propertiesFile.getFileName().toString();
    name = name.substring(0, name.length() - IdeContext.EXT_PROPERTIES.length());
    try (Reader reader = Files.newBufferedReader(propertiesFile)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load properties " + propertiesFile, e);
    }
    String id = getString(properties, "id", "plugin_id");
    String url = getString(properties, "url", "plugin_url");
    if (needUrl && ((url == null) || url.isBlank())) {
      logger.warning("Missing plugin URL in {}", propertiesFile);
    }
    boolean active = getBoolean(properties, "active", "plugin_active", propertiesFile, logger);
    String tagsCsv = getString(properties, "tags", "plugin_tags");
    Set<Tag> tags = Tag.parseCsv(tagsCsv);
    return new PluginDescriptorImpl(id, name, url, active, tags);
  }

  private static boolean getBoolean(Properties properties, String key, String legacyKey, Path propertiesFile,
      IdeLogger logger) {

    String value = getString(properties, key, legacyKey);
    if (value == null) {
      return false;
    }
    String lower = value.toLowerCase(Locale.ROOT);
    if ("true".equals(lower)) {
      return true;
    } else if ("false".equals(lower)) {
      return false;
    }
    logger.warning("Invalid boolean value '{}' for property '{}' in {}", value, key, propertiesFile);
    return false;
  }

  private static String getString(Properties properties, String key, String legacyKey) {

    String value = properties.getProperty(key);
    if (value == null) {
      value = properties.getProperty(legacyKey);
    }
    return value;
  }

}
