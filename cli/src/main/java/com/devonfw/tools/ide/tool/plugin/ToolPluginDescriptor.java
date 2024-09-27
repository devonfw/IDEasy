package com.devonfw.tools.ide.tool.plugin;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Implementation of {@link ToolPluginDescriptor}.
 *
 * @param id the unique identifier of the plugin.
 * @param name the name of the plugin properties file excluding the extension.
 * @param url the optional plugin URL (download/update site).
 * @param active {@code true} if the plugin is active and shall be installed automatically, {@code false} otherwise.
 * @param tags the {@link #tags () tags}.
 */
public record ToolPluginDescriptor(String id, String name, String url, boolean active, Set<Tag> tags) implements Tags {

  @Override
  public Set<Tag> getTags() {

    return this.tags;
  }

  /**
   * @param propertiesFile the {@link Path} to the plugin {@link Properties} file.
   * @param logger the {@link IdeLogger}.
   * @param needUrl - {@code true} if {@link ToolPluginDescriptor#url () URL} needs to be present and a warning shall be logged if missing, {@code false}
   *     otherwise.
   * @return the loaded {@link ToolPluginDescriptor}.
   */
  public static ToolPluginDescriptor of(Path propertiesFile, IdeLogger logger, boolean needUrl) {

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
    return new ToolPluginDescriptor(id, name, url, active, tags);
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
