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
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.security.ToolVulnerabilities;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container representing data from the "security.json" file with all {@link Cve CVE}s of a specific tool.
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class ToolSecurity implements JsonObject {

  private static final Logger LOG = LoggerFactory.getLogger(ToolSecurity.class);

  static final String PROPERTY_ISSUES = "issues";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final ToolSecurity EMPTY = new ToolSecurity(Map.of());

  private final Map<String, Cve> cveMap;

  private final Collection<Cve> issues;

  /**
   * The constructor.
   */
  public ToolSecurity() {
    this(new TreeMap<>());
  }

  /**
   * The constructor.
   *
   * @param issues the list of {@link Cve}s.
   */
  public ToolSecurity(List<Cve> issues) {
    this();
    setIssues(issues);
  }

  private ToolSecurity(Map<String, Cve> cveMap) {
    super();
    this.cveMap = cveMap;
    this.issues = Collections.unmodifiableCollection(this.cveMap.values());
  }

  /**
   * @return the {@link Collection} of {@link Cve}s.
   */
  public Collection<Cve> getIssues() {
    return this.issues;
  }

  /**
   * @param issues the list of {@link Cve}s.
   */
  public void setIssues(List<Cve> issues) {

    this.cveMap.clear();
    for (Cve issue : issues) {
      addIssue(issue);
    }
  }

  /**
   * @param issue the {@link Cve} to add.
   * @return {@code true} if this {@link ToolSecurity} was modified (issue added or merged), {@code false} otherwise ({@link Cve} was already contained).
   */
  public boolean addIssue(Cve issue) {

    Cve newIssue = issue;
    String id = issue.id();
    Cve existingIssue = this.cveMap.get(id);
    if (existingIssue != null) {
      newIssue = existingIssue.merge(issue);
      if (newIssue.equals(existingIssue)) {
        return false;
      }
    }
    this.cveMap.put(id, newIssue);
    return true;
  }

  /**
   * Clears all issues.
   */
  public void clearIssues() {
    this.cveMap.clear();
  }

  /**
   * Finds all {@link Cve}s for the given {@link VersionIdentifier} that also match the given {@link Predicate}.
   *
   * @param version the {@link VersionIdentifier} to check.
   * @param predicate the {@link Predicate} deciding which matching {@link Cve}s are {@link Predicate#test(Object) accepted}.
   * @return all {@link Cve}s for the given {@link VersionIdentifier}.
   */
  public ToolVulnerabilities findCves(VersionIdentifier version, Predicate<Cve> predicate) {
    List<Cve> cvesOfVersion = new ArrayList<>();
    for (Cve cve : this.issues) {
      for (VersionRange range : cve.versions()) {
        if (range.contains(version)) {
          if (predicate.test(cve)) {
            cvesOfVersion.add(cve);
          } else {
            LOG.info("Ignoring CVE {} with severity {}", cve.id(), cve.severity());
          }
        }
      }
    }
    return ToolVulnerabilities.of(cvesOfVersion);
  }

  /**
   * Finds all {@link Cve}s for the given {@link VersionIdentifier} and {@code minSeverity}.
   *
   * @param version the {@link VersionIdentifier} to check.
   * @param minSeverity the {@link IdeVariables#CVE_MIN_SEVERITY minimum severity}.
   * @return the {@link ToolVulnerabilities} for the given {@link VersionIdentifier}.
   */
  public ToolVulnerabilities findCves(VersionIdentifier version, double minSeverity) {
    return findCves(version, cve -> cve.severity() >= minSeverity);
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
