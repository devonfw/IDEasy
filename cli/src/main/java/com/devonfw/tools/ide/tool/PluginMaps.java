package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper class for holding two maps of plugin descriptors: one keyed by plugin ID and the other keyed by plugin name.
 */
public class PluginMaps {
  private final Map<String, PluginDescriptor> mapById;
  private final Map<String, PluginDescriptor> mapByName;

  
  private final IdeContext context;

  /**
   * The constructor.
   * @param context the {@link IdeContext}.
   */
  public PluginMaps(IdeContext context) {
    super();
    this.context = context;
    this.mapById = new HashMap<>();
    this.mapByName = new HashMap<>();
  }

  /**
   * @return the {@link PluginDescriptor} for the given {@link PluginDescriptor#getId() ID} or {@code null} if not found.
   */
  public PluginDescriptor getById(String id) {
    return this.mapById.get(id);
  }

  /**
   * @return the {@link PluginDescriptor} for the given {@link PluginDescriptor#getName() name} or {@code null} if not found.
   */
  public PluginDescriptor getByName(String name) {
    return this.mapByName.get(name);
  }

  /**
   * @return the immutable {@link Collection} of {@link PluginDescriptor}s configured for this IDE tool.
   */
  public Collection<PluginDescriptor> getPlugins() {
    
    Map<String, PluginDescriptor> map = this.mapById;
    if (map.isEmpty()) {
      map = this.mapByName; // potentially plugins for this tool have no ID
    }
    return Collections.unmodifiableCollection(map.values());
  }
  
  /**
   * Registers a {@link PluginDescriptor} to this map.
   */
  protected void add(PluginDescriptor descriptor) {
    put(descriptor.getName(), descriptor, this.mapByName);
    String id = descriptor.getId();
    if ((id != null) && !id.isEmpty()) {
      put(id, descriptor, this.mapById);
    }
  }
  
  public void put(String key, PluginDescriptor descriptor, Map<String, PluginDescriptor> map) {
    PluginDescriptor duplicate = map.put(key, descriptor);
    if (duplicate != null) {
      this.context.info("Plugin with key {} was {} but got overridden by {}", key, duplicate, descriptor);
    }
  }
}
