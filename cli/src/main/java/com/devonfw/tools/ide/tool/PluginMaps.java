package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.tool.ide.PluginDescriptor;

import java.util.Map;

/**
 * A wrapper class for holding two maps of plugin descriptors: one keyed by plugin ID and the other keyed by plugin name.
 */
public class PluginMaps {
  private final Map<String, PluginDescriptor> mapById;
  private final Map<String, PluginDescriptor> mapByName;

  /**
   * Constructs a new PluginMaps object with the specified maps.
   *
   * @param mapById   A map with plugin IDs as keys and corresponding plugin descriptors as values.
   * @param mapByName A map with plugin names as keys and corresponding plugin descriptors as values.
   */
  public PluginMaps(Map<String, PluginDescriptor> mapById, Map<String, PluginDescriptor> mapByName) {
    this.mapById = mapById;
    this.mapByName = mapByName;
  }

  /**
   * Returns the map of plugin descriptors keyed by plugin ID.
   *
   * @return The map of plugin descriptors keyed by plugin ID.
   */
  public Map<String, PluginDescriptor> getById() {
    return mapById;
  }

  /**
   * Returns the map of plugin descriptors keyed by plugin name.
   *
   * @return The map of plugin descriptors keyed by plugin name.
   */
  public Map<String, PluginDescriptor> getByName() {
    return mapByName;
  }
}
