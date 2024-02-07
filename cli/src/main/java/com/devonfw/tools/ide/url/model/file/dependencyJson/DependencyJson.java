package com.devonfw.tools.ide.url.model.file.dependencyJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent the Json file for dependencies to be installed.
 */
public class DependencyJson {
  private final Map<VersionRange, List<DependencyInfo>> dependencies;

  /**
   * The constructor.
   */
  public DependencyJson() {

    this.dependencies = new LinkedHashMap<>();
  }

  /**
   * @return the {@link Map} that maps the VersionRange of the tool to the list of {@link DependencyInfo}
   */
  public Map<VersionRange, List<DependencyInfo>> getDependencies() {

    return this.dependencies;
  }
}
