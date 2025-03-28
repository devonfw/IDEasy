package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container representing data from the "security.json".
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class ToolSecurity {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolSecurity EMPTY = new ToolSecurity(Collections.emptyList());
  private List<CVE> issues;


  private ToolSecurity() {
    super();
  }

  private ToolSecurity(List<CVE> issues) {

    super();
    this.issues = issues;
  }

  /**
   * @param version the {@link VersionIdentifier} of the tool to install.
   * @return The {@link List} of {@link CVE}s for the given tool version.
   */
  public List<CVE> findCVEs(VersionIdentifier version) {
    List<CVE> cves = new ArrayList<>();
    for (CVE cve : issues) {
      for (VersionRange versionRange : cve.versions()) {
        if (versionRange.contains(version)) {
          cves.add(cve);
        }
      }
    }
    return cves;
  }

  /**
   * @param file the {@link Path} to the JSON file to load.
   * @return the loaded {@link ToolSecurity} or the {@link #getEmpty() empty instance} if given {@link Path} does not exist.
   */
  public static ToolSecurity of(Path file) {

    if (Files.exists(file)) {
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        return MAPPER.readValue(reader, ToolSecurity.class);
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

  /**
   * @return the list of CVEs
   */
  public List<CVE> getIssues() {
    return issues;
  }

  /**
   * @param issues the list of CVEs
   */
  public void setIssues(List<CVE> issues) {
    this.issues = issues;
  }
}
