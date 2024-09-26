package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container representing data from the "dependencies.json".
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlDependencyFile
 */
public class ToolDependencies {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolDependencies EMPTY = new ToolDependencies(Collections.emptyMap());
  private final Map<VersionRange, List<ToolDependency>> dependencies;

  private ToolDependencies(Map<VersionRange, List<ToolDependency>> dependencies) {

    super();
    this.dependencies = dependencies;
  }

  /**
   * @param version the {@link VersionIdentifier} of the tool to install.
   * @return The {@link List} of {@link ToolDependency}s for the given tool version.
   */
  public List<ToolDependency> findDependencies(VersionIdentifier version) {

    for (Map.Entry<VersionRange, List<ToolDependency>> map : this.dependencies.entrySet()) {
      VersionRange versionRange = map.getKey();
      if (versionRange.contains(version)) {
        return map.getValue();
      }
    }
    return Collections.emptyList();
  }

  /**
   * @param file the {@link Path} to the JSON file to load.
   * @return the loaded {@link ToolDependencies} or the {@link #getEmpty() empty instance} if given {@link Path} does not exist.
   */
  public static ToolDependencies of(Path file) {

    if (Files.exists(file)) {
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        TypeReference<HashMap<VersionRange, List<ToolDependency>>> typeRef = new TypeReference<>() {
        };
        Map<VersionRange, List<ToolDependency>> dependencies = MAPPER.readValue(reader, typeRef);
        return new ToolDependencies(dependencies);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + file, e);
      }
    } else {
      return EMPTY;
    }
  }

  /**
   * @return the empty instance of {@link ToolDependencies}.
   */
  public static ToolDependencies getEmpty() {

    return EMPTY;
  }

}
