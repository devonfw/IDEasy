package com.devonfw.ide.gui.settings;

import java.util.List;
import java.util.Objects;

/**
 * Model representing the configurable settings for a single tool.
 */
public final class ToolConfiguration {

  private final String toolName;

  /** Group/type of the tool used for grouping in the UI. */
  private ToolGroup group;

  private boolean enabled;

  private String configuredVersion;

  private String configuredEdition;

  private boolean supportsEdition;

  private List<String> availableEditions;

  private List<String> availableVersions;

  //<------------ Constructor & Getters/Setters ------------>

  public ToolConfiguration(String toolName) {
    this.toolName = Objects.requireNonNull(toolName);
  }

  /**
   * Tool groups used to group/sort tools in the UI. Order of declaration determines the default ordering.
   */
  public enum ToolGroup {
    IDE("IDE tools"),
    LOCAL("Local tools"),
    GLOBAL("Global tools"),
    PIP("Pip-based tools"),
    NPM("NPM-based tools"),
    OTHER("Other tools");

    private final String label;

    ToolGroup(String label) {
      this.label = label;
    }

    public String getLabel() {
      return this.label;
    }
  }

  public String getToolName() {
    return this.toolName;
  }

  public ToolGroup getGroup() {
    return this.group;
  }

  public void setGroup(ToolGroup group) {
    this.group = group;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getConfiguredVersion() {
    return this.configuredVersion;
  }

  public void setConfiguredVersion(String configuredVersion) {
    this.configuredVersion = configuredVersion;
  }

  public String getConfiguredEdition() {
    return this.configuredEdition;
  }

  public void setConfiguredEdition(String configuredEdition) {
    this.configuredEdition = configuredEdition;
  }

  public boolean isSupportsEdition() {
    return this.supportsEdition;
  }

  public void setSupportsEdition(boolean supportsEdition) {
    this.supportsEdition = supportsEdition;
  }

  public List<String> getAvailableEditions() {
    return this.availableEditions;
  }

  public void setAvailableEditions(List<String> availableEditions) {
    this.availableEditions = availableEditions;
  }

  public List<String> getAvailableVersions() {
    return this.availableVersions;
  }

  public void setAvailableVersions(List<String> availableVersions) {
    this.availableVersions = availableVersions;
  }

}

