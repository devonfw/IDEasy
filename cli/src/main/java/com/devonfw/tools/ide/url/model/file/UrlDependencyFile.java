package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} for the "dependency.json" file.
 */
public class UrlDependencyFile extends AbstractUrlFile<UrlEdition> {

  public static final String DEPENDENCY_JSON = "dependencies.json";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private Map<VersionRange, List<DependencyInfo>> dependencyMap;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlDependencyFile(UrlEdition parent) {

    super(parent, DEPENDENCY_JSON);
  }

  /**
   * @return the content of the dependency map of the dependency.json file
   */
  public Map<VersionRange, List<DependencyInfo>> getDependencyMap() {

    return this.dependencyMap;
  }

  public List<DependencyInfo> findDependenciesFromJson(Map<VersionRange, List<DependencyInfo>> dependencyMap, VersionIdentifier toolVersionToCheck) {

    for (Map.Entry<VersionRange, List<DependencyInfo>> map : dependencyMap.entrySet()) {

      VersionRange foundToolVersionRange = map.getKey();

      if (foundToolVersionRange.contains(toolVersionToCheck)) {
        return map.getValue();
      }
    }
    return null;
  }

  public boolean isDependencyMapNull() {
    return dependencyMap == null;
  }

  @Override
  protected void doLoad() {

    Path path = getPath();
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {

        TypeReference<HashMap<VersionRange, List<DependencyInfo>>> typeRef = new TypeReference<>() {
        };
        this.dependencyMap = MAPPER.readValue(reader, typeRef);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    } else {
      this.dependencyMap = new HashMap<>();
    }
  }

  @Override
  protected void doSave() {

  }
}
