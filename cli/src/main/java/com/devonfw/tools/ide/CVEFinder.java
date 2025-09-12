package com.devonfw.tools.ide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.json.CVE;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Class used to handle and get {@link ToolSecurity}, {@link CVE} for specific tool.
 */
public class CVEFinder {

  private final IdeContext context;
  private final ToolCommandlet tool;
  private final ToolSecurity toolSecurity;
  private final List<VersionIdentifier> allVersions;
  private final VersionIdentifier version;
  private final List<CVE> cves;

  /**
   * The constructor
   *
   * @param context the {@link IdeContext}
   * @param tool the {@link ToolCommandlet}
   * @param version the {@link VersionIdentifier} of the tool.
   */
  public CVEFinder(IdeContext context, ToolCommandlet tool, VersionIdentifier version) {
    this.context = context;
    this.tool = tool;
    this.toolSecurity = context.getDefaultToolRepository().findSecurity(tool.getName(), tool.getConfiguredEdition());
    this.allVersions = tool.getVersions();
    this.version = version;
    this.cves = toolSecurity.getIssues();
  }

  /**
   * The constructor
   *
   * @param context the {@link IdeContext}
   * @param tool the {@link ToolCommandlet}
   * @param version the {@link VersionIdentifier} of the tool.
   * @param allowedVersions the {@link VersionRange} that is used to select version suggestions.
   */
  public CVEFinder(IdeContext context, ToolCommandlet tool, VersionIdentifier version, VersionRange allowedVersions) {
    this.context = context;
    this.tool = tool;
    this.toolSecurity = context.getDefaultToolRepository().findSecurity(tool.getName(), tool.getConfiguredEdition());
    List<VersionIdentifier> filterdAllVersions = new ArrayList<>();
    for (VersionIdentifier toolVersion : tool.getVersions()) {
      if (allowedVersions.contains(toolVersion)) {
        filterdAllVersions.add(toolVersion);
      }
    }
    this.allVersions = filterdAllVersions;
    this.version = version;
    this.cves = toolSecurity.getIssues();
  }

  /**
   * Prints all {@link CVE}s from a specific {@link VersionIdentifier}
   *
   * @param versionIdentifier {@link VersionIdentifier}.
   */
  public void listCVEs(VersionIdentifier versionIdentifier) {
    if (getCVEs(versionIdentifier).size() == 0) {
      context.info("No CVEs found for this version");
    }
    for (CVE cve : getCVEs(versionIdentifier)) {
      context.warning("CVE_ID: " + cve.id());
      context.warning("Severity: " + String.valueOf(cve.severity()));
      context.warning("Affected versions: " + cve.versions().toString());
      context.warning("Visit https://nvd.nist.gov/vuln/detail/" + cve.id() + " for more information");
    }
  }

  /**
   * @param versionIdentifier
   * @return all {@link CVE}s from specific {@link VersionIdentifier}.
   */
  public Collection<CVE> getCVEs(VersionIdentifier versionIdentifier) {
    List<CVE> cvesOfVersion = new ArrayList<>();
    for (CVE cve : cves) {
      for (VersionRange range : cve.versions()) {
        if (range.contains(versionIdentifier) && cve.severity() >= IdeVariables.CVE_MIN_SEVERIRY.get(context)) {
          cvesOfVersion.add(cve);
        }
      }
    }
    return cvesOfVersion;
  }

  /**
   * @return the safest latest {@link VersionIdentifier} from this {@link ToolSecurity}.
   */
  public VersionIdentifier findSafestLatestVersion() {
    VersionIdentifier safestLatestVersion = allVersions.getFirst();
    double severitySumLatestVersion = severitySum(getCVEs(safestLatestVersion));
    double distancePunishment = (1d / allVersions.size()) * 5;
    for (int i = 0; i < allVersions.size(); i++) {
      VersionIdentifier current = allVersions.get(i);
      double severitySumcurrent = severitySum(getCVEs(current));
      if (severitySumcurrent == 0) {
        return current;
      }
      if (severitySumLatestVersion >= distancePunishment * i + severitySumcurrent) {
        safestLatestVersion = current;
        severitySumLatestVersion = severitySumcurrent + distancePunishment * i;
      }
    }
    return safestLatestVersion;
  }

  /**
   * @return the safest nearest {@link VersionIdentifier} from this {@link ToolSecurity}.
   */
  public VersionIdentifier findSafestNearestVersion() {
    VersionIdentifier safestNearestVersion = version;
    double severitySumNearestVersion = severitySum(getCVEs(safestNearestVersion));
    double distancePunishment = (1d / allVersions.size()) * 5;
    int upIndex = allVersions.indexOf(version);
    int downIndex = upIndex;
    for (int i = 1; i < allVersions.size() - allVersions.indexOf(version) || i < allVersions.indexOf(version); i++) {
      if (upIndex < allVersions.size() - allVersions.indexOf(version)) {
        upIndex++;
      }
      if (downIndex >= 0) {
        downIndex--;
      }
      VersionIdentifier upVersion = allVersions.get(upIndex);
      VersionIdentifier downVersion = allVersions.get(downIndex);
      double severityUp = severitySum(getCVEs(upVersion));
      double severityDown = severitySum(getCVEs(downVersion));
      if (severityUp == 0) {
        return upVersion;
      }
      if (severityDown == 0) {
        return downVersion;
      }
      if (severitySumNearestVersion >= distancePunishment * i + severityUp) {
        safestNearestVersion = upVersion;
        severitySumNearestVersion = severityUp + distancePunishment * i;
      } else if (severitySumNearestVersion >= distancePunishment * i + severityDown) {
        safestNearestVersion = downVersion;
        severitySumNearestVersion = severityDown + distancePunishment * i;
      }
    }
    return safestNearestVersion;
  }

  /**
   * @param cves list of {@link CVE}s
   * @return sum of {@link CVE#severity()}
   */
  public static double severitySum(Collection<CVE> cves) {
    double severitySum = 0;
    for (CVE cve : cves) {
      severitySum += cve.severity();
    }
    return severitySum;
  }

}
