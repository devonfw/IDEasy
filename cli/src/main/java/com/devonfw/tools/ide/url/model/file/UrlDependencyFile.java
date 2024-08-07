package com.devonfw.tools.ide.url.model.file;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public Map<VersionRange, List<DependencyInfo>> getDependencyMap() {

    return this.dependencyMap;
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
