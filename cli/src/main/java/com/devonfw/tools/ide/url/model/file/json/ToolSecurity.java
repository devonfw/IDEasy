package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container representing data from the "security.json".
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class ToolSecurity {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolSecurity EMPTY = new ToolSecurity(Collections.emptyMap(), Path.of("empty"));
  private final Map<String, List<CVE>> security;

  private final Path path;

  private ToolSecurity(Map<String, List<CVE>> security, Path path) {

    super();
    this.security = security;
    this.path = path;
  }

  /**
   * @param version the {@link VersionIdentifier} of the tool to install.
   * @return The {@link List} of {@link CVE}s for the given tool version.
   */
  public List<CVE> findCVEs(VersionIdentifier version, IdeLogger logger) {
    Collection<List<CVE>> values = this.security.values();
    List<CVE> cves = new ArrayList<>();
    for (List<CVE> entry : values) {
      for (CVE cve : entry) {
        for (VersionRange versionRange : cve.versions()) {
          if (versionRange.contains(version)) {
            cves.add(cve);
          }
        }
      }
    }
    if (!cves.isEmpty()) {
      return cves;
    }
    int size = security.size();
    if (size > 0) {
      logger.warning("No match for version {} while {} version ranges are configured in {} - configuration error?!", version, size, this.path);
    }
    return Collections.emptyList();
  }

  /**
   * @param file the {@link Path} to the JSON file to load.
   * @return the loaded {@link ToolSecurity} or the {@link #getEmpty() empty instance} if given {@link Path} does not exist.
   */
  public static ToolSecurity of(Path file) {

    if (Files.exists(file)) {
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        TypeReference<TreeMap<String, List<CVE>>> typeRef = new TypeReference<>() {
        };
        Map<String, List<CVE>> security = MAPPER.readValue(reader, typeRef);
        return new ToolSecurity(security, file);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + file, e);
      }
    } else {
      return EMPTY;
    }
  }

  /**
   * @return the empty instance of {@link ToolSecurity}.
   */
  public static ToolSecurity getEmpty() {

    return EMPTY;
  }
}
