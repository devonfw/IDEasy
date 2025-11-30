package com.devonfw.tools.ide.url.model.file.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
 * @see ToolSecurity
 */
public record Cve(String id, double severity, List<VersionRange> versions) {

  static final String PROPERTY_ID = "id";

  static final String PROPERTY_SEVERITY = "severity";

  static final String PROPERTY_VERSIONS = "versions";

  public Cve {
    Objects.requireNonNull(id);
    Objects.requireNonNull(versions);
    assert !versions.isEmpty();
  }

  /**
   * @param cves the {@link Cve}s to summarize.
   * @return the sum of {@link Cve#severity()}.
   */
  public static double severitySum(Collection<Cve> cves) {
    double severitySum = 0;
    for (Cve cve : cves) {
      severitySum += cve.severity();
    }
    return severitySum;
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
    return new Cve(this.id, this.severity, newVersions);
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
}
