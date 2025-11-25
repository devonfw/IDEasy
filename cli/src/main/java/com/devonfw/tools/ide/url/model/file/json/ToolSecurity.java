package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container representing data from the "security.json" file with all {@link Cve CVE}s of a specific tool.
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class ToolSecurity {

  static final String PROPERTY_ISSUES = "issues";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolSecurity EMPTY = new ToolSecurity(Collections.emptyList());

  private List<Cve> issues;

  /**
   * The constructor.
   */
  public ToolSecurity() {
    this(new ArrayList<>());
  }

  /**
   * The constructor.
   *
   * @param issues the {@link List} of {@link Cve CVE}s.
   */
  public ToolSecurity(List<Cve> issues) {

    super();
    this.issues = issues;
  }

  /**
   * @return the list of CVEs
   */
  public List<Cve> getIssues() {
    return issues;
  }

  /**
   * @param issues the list of CVEs
   */
  public void setIssues(List<Cve> issues) {
    this.issues = issues;
  }

  /**
   * Finds all {@link Cve}s for the given {@link VersionIdentifier} that also match the given {@link Predicate}.
   *
   * @param version the {@link VersionIdentifier} to check.
   * @param predicate the {@link Predicate} deciding which matching {@link Cve}s are {@link Predicate#test(Object) accepted}.
   * @return all {@link Cve}s for the given {@link VersionIdentifier}.
   */
  public Collection<Cve> findCves(VersionIdentifier version, IdeLogger logger, Predicate<Cve> predicate) {
    List<Cve> cvesOfVersion = new ArrayList<>();
    for (Cve cve : this.issues) {
      for (VersionRange range : cve.versions()) {
        if (range.contains(version)) {
          if (predicate.test(cve)) {
            cvesOfVersion.add(cve);
          } else {
            logger.info("Ignoring CVE {} with severity {}", cve.id(), cve.severity());
          }
        }
      }
    }
    return cvesOfVersion;
  }

  /**
   * Finds all {@link Cve}s for the given {@link VersionIdentifier} and {@code minSeverity}.
   *
   * @param version the {@link VersionIdentifier} to check.
   * @param minSeverity the {@link IdeVariables#CVE_MIN_SEVERITY minimum severity}.
   * @return all {@link Cve}s for the given {@link VersionIdentifier}.
   */
  public Collection<Cve> findCves(VersionIdentifier version, IdeLogger logger, double minSeverity) {
    return findCves(version, logger, cve -> cve.severity() >= minSeverity);
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
}
