package com.devonfw.tools.ide.url.model.file.json;

import java.util.Collection;
import java.util.List;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a CVE (common vulnerabilities and exposures) of a tool.
 *
 * @param id the unique identifier (e.g. "CVE-2021-44228").
 * @param severity the severity in the range from (0,10.0] where 10.0 is most critical.
 * @param versions the {@link VersionRange}s of the affected versions. Typically one entry but might also affect multiple ranges (e.g. "[1.0,1.2)" and
 *     "[2.0,2.2)"). Should never be {@code null} or {@link List#isEmpty() empty}.
 * @see ToolSecurity
 */
public record Cve(String id, double severity, List<VersionRange> versions) {

  static final String PROPERTY_ID = "id";

  static final String PROPERTY_SEVERITY = "severity";

  static final String PROPERTY_VERSIONS = "versions";

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
}
