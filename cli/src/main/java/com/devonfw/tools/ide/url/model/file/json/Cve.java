package com.devonfw.tools.ide.url.model.file.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.devonfw.tools.ide.version.VersionRangeRelation;

/**
 * Model to represent a CVE (common vulnerabilities and exposures) of a tool.
 *
 * @param id the unique identifier (e.g. "CVE-2021-44228").
 * @param severity the severity in the range from (0,10.0] where 10.0 is most critical.
 * @param versions the {@link VersionRange}s of the affected versions. Typically one entry but might also affect multiple ranges. E.g. "[1.0,1.2)" and
 *     "[2.0,2.2)". Should never be {@code null} or {@link List#isEmpty() empty}.
 * @param conditions the additional {@link VersionRange}s of affected versions per {@link OperatingSystem#toString() operating system}. Only relevant when the
 *     end-user runs IDEasy on the matching operating system. Never {@code null} but may be {@link Map#isEmpty() empty}.
 * @see ToolSecurity
 */
public record Cve(String id, double severity, List<VersionRange> versions, Map<String, List<VersionRange>> conditions) implements JsonObject {

  static final String PROPERTY_ID = "id";

  static final String PROPERTY_SEVERITY = "severity";

  static final String PROPERTY_VERSIONS = "versions";

  static final String PROPERTY_CONDITIONS = "conditions";

  public Cve {
    Objects.requireNonNull(id);
    Objects.requireNonNull(versions);
    assert !versions.isEmpty();
    if (conditions == null) {
      conditions = Map.of();
    }
  }

  /**
   * @param id the {@link #id()}.
   * @param severity the {@link #severity()}.
   * @param versions the {@link #versions()}.
   */
  public Cve(String id, double severity, List<VersionRange> versions) {

    this(id, severity, versions, Map.of());
  }

  /**
   * @param version the {@link VersionIdentifier} to check.
   * @param os the current {@link OperatingSystem} (may be {@code null}).
   * @return {@code true} if the given {@link VersionIdentifier} is affected by this CVE on the given {@link OperatingSystem}, {@code false} otherwise.
   */
  public boolean isAffected(VersionIdentifier version, OperatingSystem os) {

    if (contains(this.versions, version)) {
      return true;
    }
    return (os != null) && contains(this.conditions.get(os.toString()), version);
  }

  private static boolean contains(List<VersionRange> ranges, VersionIdentifier version) {

    if (ranges != null) {
      for (VersionRange range : ranges) {
        if (range.contains(version)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @param issue the {@link Cve} to merge with. Has to have the same {@link #id()} and {@link #severity()}.
   * @return the merged {@link Cve}.
   */
  public Cve merge(Cve issue) {

    if (!this.id.equals(issue.id)) {
      throw new IllegalArgumentException(this.id + " != " + issue.id);
    }
    if (this.severity != issue.severity) {
      throw new IllegalArgumentException(this.severity + " != " + issue.severity + " - cannot merge " + this.id);
    }
    List<VersionRange> newVersions = new ArrayList<>(this.versions);
    for (VersionRange versionRange : issue.versions) {
      mergeVersionRage(newVersions, versionRange);
    }
    return new Cve(this.id, this.severity, newVersions, mergeConditions(issue.conditions));
  }

  private Map<String, List<VersionRange>> mergeConditions(Map<String, List<VersionRange>> other) {

    if (this.conditions.isEmpty() && other.isEmpty()) {
      return Map.of();
    }
    Map<String, List<VersionRange>> newConditions = new TreeMap<>();
    this.conditions.forEach((os, ranges) -> newConditions.put(os, new ArrayList<>(ranges)));
    other.forEach((os, ranges) -> {
      List<VersionRange> newRanges = newConditions.computeIfAbsent(os, key -> new ArrayList<>());
      ranges.forEach(range -> mergeVersionRage(newRanges, range));
    });
    return newConditions;
  }

  /**
   * @param newVersions the {@link List} of {@link VersionRange}s.
   * @param versionRange the new {@link VersionRange} to add.
   */
  public static void mergeVersionRage(List<VersionRange> newVersions, VersionRange versionRange) {

    if (newVersions.isEmpty()) {
      newVersions.add(versionRange);
      return;
    }
    VersionIdentifier min = versionRange.getMin();
    int insertIndex = 0;
    boolean removed = false;
    VersionRange current = versionRange;
    Iterator<VersionRange> versionIterator = newVersions.iterator();
    while (versionIterator.hasNext()) {
      VersionRange range = versionIterator.next();
      VersionRange merged = range.union(current, VersionRangeRelation.CONNECTED_LOOSELY);
      if (merged != null) {
        current = merged;
        versionIterator.remove();
      } else if (!removed && (min != null) && min.isGreater(range.getMin())) {
        insertIndex++;
      }
    }
    newVersions.add(insertIndex, current);
  }

  @Override
  public String toString() {

    return this.id + " with severity " + this.severity + " and affected versions: " + this.versions + "\nhttps://nvd.nist.gov/vuln/detail/" + this.id;
  }
}
