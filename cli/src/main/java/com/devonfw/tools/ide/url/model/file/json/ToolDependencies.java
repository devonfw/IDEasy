package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(ToolDependencies.class);

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolDependencies EMPTY = new ToolDependencies(Collections.emptyMap(), Path.of("empty"));
  private final Map<VersionRange, List<ToolDependency>> dependencies;

  private final Path path;

  private ToolDependencies(Map<VersionRange, List<ToolDependency>> dependencies, Path path) {

    super();
    this.dependencies = dependencies;
    this.path = path;
  }

  /**
   * @param version the {@link VersionIdentifier} of the tool to install.
   * @return The {@link List} of {@link ToolDependency}s for the given tool version.
   */
  public List<ToolDependency> findDependencies(VersionIdentifier version) {

    for (Map.Entry<VersionRange, List<ToolDependency>> entry : this.dependencies.entrySet()) {
      VersionRange versionRange = entry.getKey();
      if (versionRange.contains(version)) {
        return entry.getValue();
      }
    }
    int size = dependencies.size();
    if (size > 0) {
      LOG.warn("No match for version {} while {} version ranges are configured in {} - configuration error?!", version, size, this.path);
    }
    return Collections.emptyList();
  }

  @Override
  public String toString() {

    if (this == EMPTY) {
      return "[empty]";
    }
    return this.path.toString();
  }

  /**
   * @param file the {@link Path} to the JSON file to load.
   * @return the loaded {@link ToolDependencies} or the {@link #getEmpty() empty instance} if given {@link Path} does not exist.
   */
  public static ToolDependencies of(Path file) {

    if (Files.exists(file)) {
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        TypeReference<TreeMap<VersionRange, List<ToolDependency>>> typeRef = new TypeReference<>() {
        };
        Map<VersionRange, List<ToolDependency>> dependencies = MAPPER.readValue(reader, typeRef);
        return new ToolDependencies(dependencies, file);
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
