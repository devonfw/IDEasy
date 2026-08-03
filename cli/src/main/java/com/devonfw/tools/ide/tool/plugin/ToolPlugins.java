package com.devonfw.tools.ide.tool.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class for holding two maps of plugin descriptors: one keyed by plugin ID and the other keyed by plugin name.
 */
public class ToolPlugins {

  private static final Logger LOG = LoggerFactory.getLogger(ToolPlugins.class);

  private final Map<String, ToolPluginDescriptor> mapById;
  private final Map<String, ToolPluginDescriptor> mapByName;

  /**
   * The constructor.
   */
  public ToolPlugins() {
    super();
    this.mapById = new HashMap<>();
    this.mapByName = new HashMap<>();
  }

  /**
   * @param id the {@link ToolPluginDescriptor#id() ID} of the requested {@link ToolPluginDescriptor}.
   * @return the {@link ToolPluginDescriptor} for the given {@link ToolPluginDescriptor#id() ID} or {@code null} if not found.
   */
  public ToolPluginDescriptor getById(String id) {
    return this.mapById.get(id);
  }

  /**
   * @param name the {@link ToolPluginDescriptor#name() name} of the requested {@link ToolPluginDescriptor}.
   * @return the {@link ToolPluginDescriptor} for the given {@link ToolPluginDescriptor#name() name} or {@code null} if not found.
   */
  public ToolPluginDescriptor getByName(String name) {
    return this.mapByName.get(name);
  }

  /**
   * @return the immutable {@link Collection} of {@link ToolPluginDescriptor}s configured for this IDE tool.
   */
  public Collection<ToolPluginDescriptor> getPlugins() {

    Map<String, ToolPluginDescriptor> map = this.mapById;
    if (map.isEmpty()) {
      map = this.mapByName; // potentially plugins for this tool have no ID
    }
    return Collections.unmodifiableCollection(map.values());
  }

  /**
   * @param descriptor the {@link ToolPluginDescriptor} to add.
   */
  protected void add(ToolPluginDescriptor descriptor) {
    put(descriptor.name(), descriptor, this.mapByName);
    String id = descriptor.id();
    if ((id != null) && !id.isEmpty()) {
      put(id, descriptor, this.mapById);
    }
  }

  private void put(String key, ToolPluginDescriptor descriptor, Map<String, ToolPluginDescriptor> map) {
    ToolPluginDescriptor duplicate = map.put(key, descriptor);
    if (duplicate != null) {
      LOG.info("Plugin with key {} was {} but got overridden by {}", key, duplicate, descriptor);
    }
  }
}
