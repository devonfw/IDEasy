package com.devonfw.tools.ide.url.model.file.dependencyJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DependencyJson {
  private final Map<String, List<DependencyInfo>> dependencies;

  public DependencyJson() {

    this.dependencies = new LinkedHashMap<>();
  }

  public Map<String, List<DependencyInfo>> getDependencies() {

    return this.dependencies;
  }
}
