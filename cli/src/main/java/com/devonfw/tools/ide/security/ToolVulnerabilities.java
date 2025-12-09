package com.devonfw.tools.ide.security;

import java.util.Collection;
import java.util.List;

import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.version.GenericVersionRange;

/**
 * Container for {@link #getIssues() vulnerabilities} with internal scoring.
 */
public class ToolVulnerabilities implements Comparable<ToolVulnerabilities> {

  /** The empty {@link ToolVulnerabilities} instance. */
  public static final ToolVulnerabilities EMPTY = new ToolVulnerabilities(List.of());

  private final List<Cve> issues;

  private final double maxSeverity;

  private final double severitySum;

  /**
   * The constructor.
   *
   * @param issues the {@link Collection} of
   */
  private ToolVulnerabilities(Collection<Cve> issues) {
    this.issues = List.copyOf(issues);
    double max = 0;
    double sum = 0;
    for (Cve issue : issues) {
      double severity = issue.severity();
      sum += severity;
      if (severity > max) {
        max = severity;
      }
    }
    this.maxSeverity = max;
    this.severitySum = sum;
  }

  /**
   * @return the {@link Collection} of {@link Cve}s.
   */
  public Collection<Cve> getIssues() {

    return issues;
  }

  @Override
  public int compareTo(ToolVulnerabilities o) {

    if (o == null) {
      return 1;
    } else if (this.maxSeverity < o.maxSeverity) {
      return -1;
    } else if (this.maxSeverity > o.maxSeverity) {
      return 1;
    } else if (this.severitySum < o.severitySum) {
      return -1;
    } else if (this.severitySum > o.severitySum) {
      return 1;
    }
    return 0;
  }

  /**
   * @param other the {@link ToolVulnerabilities} to compare to.
   * @return {@code true} if this {@link ToolVulnerabilities} is safer than the given {@link ToolVulnerabilities}, {@code false} otherwise (equal or unsafer).
   */
  public boolean isSafer(ToolVulnerabilities other) {
    if (other == null) {
      return true;
    }
    return this.compareTo(other) < 0;
  }

  /**
   * @param other the {@link ToolVulnerabilities} to compare to.
   * @return {@code true} if this {@link ToolVulnerabilities} is safer than or equal to the given {@link ToolVulnerabilities}, {@code false} otherwise
   *     (unsafer).
   */
  public boolean isSaferOrEqual(ToolVulnerabilities other) {
    if (other == null) {
      return true;
    }
    return this.compareTo(other) <= 0;
  }

  @Override
  public String toString() {

    return toString(null);
  }

  /**
   * @param toolEditionAndVersion the optional {@link ToolEditionAndVersion}.
   * @return the {@link String} representation of this {@link ToolVulnerabilities}.
   */
  public String toString(ToolEditionAndVersion toolEditionAndVersion) {

    StringBuilder sb = new StringBuilder();
    char separator = '.';
    if (this.issues.isEmpty()) {
      sb.append("No CVEs found");
    } else {
      sb.append("Found ").append(this.issues.size()).append(" CVE(s)");
      separator = ':';
    }
    if (toolEditionAndVersion != null) {
      GenericVersionRange version = toolEditionAndVersion.getResolvedVersion();
      if (version == null) {
        version = toolEditionAndVersion.getVersion();
      }
      sb.append(" for version ").append(version).append(" of tool ").append(toolEditionAndVersion.getEdition());
    }
    sb.append(separator);
    for (Cve issue : issues) {
      sb.append('\n');
      sb.append(issue.toString());
    }
    return sb.toString();
  }

  /**
   * @param issues the {@link Collection} of {@link Cve}s.
   * @return the according {@link ToolVulnerabilities}.
   */
  public static ToolVulnerabilities of(Collection<Cve> issues) {
    if (issues.isEmpty()) {
      return EMPTY;
    }
    return new ToolVulnerabilities(issues);
  }
}
