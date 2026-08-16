package com.devonfw.ide.gui.settings;

/**
 * Model representing a single configurable plugin belonging to a {@link ToolConfiguration}.
 */
public final class PluginConfiguration implements ToolTreeNode {

  private final String pluginName;

  private final String pluginId;

  private final ToolConfiguration parent;

  private boolean active;

  public PluginConfiguration(String pluginName, String pluginId, boolean active, ToolConfiguration parent) {

    this.pluginName = pluginName;
    this.pluginId = pluginId;
    this.active = active;
    this.parent = parent;
  }

  public String getPluginName() {

    return this.pluginName;
  }

  public String getPluginId() {

    return this.pluginId;
  }

  public boolean isActive() {

    return this.active;
  }

  public void setActive(boolean active) {

    this.active = active;
  }

  /** @return {@code true} if the parent tool is enabled, so plugin checkboxes should be editable. */
  public boolean isParentEnabled() {

    return this.parent.isEnabled();
  }

  public String getParentToolName() {

    return this.parent.getToolName();
  }
}
