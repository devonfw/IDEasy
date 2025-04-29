package com.devonfw.tools.ide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.json.CVE;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

public class CVEFinder {

  IdeContext context;
  ToolCommandlet tool;
  ToolSecurity toolSecurity;
  List<VersionIdentifier> allVersions;
  VersionIdentifier version;
  List<CVE> cves;


  public CVEFinder(IdeContext context, ToolCommandlet tool, VersionIdentifier version) {
    this.context = context;
    this.tool = tool;
    this.toolSecurity = context.getDefaultToolRepository().findSecurity(tool.getName(), tool.getConfiguredEdition());
    this.allVersions = tool.getVersions();
    this.version = version;
    this.cves = toolSecurity.findCVEs(version);
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2023.1)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.0)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.0)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.0)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.0)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.0)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(2025.1)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(1.1)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(1.1)"))));
    cves.add(new CVE("Test", 2.2, Collections.singletonList(VersionRange.of("(1.1)"))));
  }

  public void listCVEs(VersionIdentifier versionIdentifier) {
    for (CVE cve : getCVEs(versionIdentifier)) {
      context.warning(cve.id());
      context.warning(String.valueOf(cve.severity()));
      context.warning(cve.versions().toString());
    }
  }

  public Collection<CVE> getCVEs(VersionIdentifier versionIdentifier) {
    List<CVE> cvesOfVersion = new ArrayList<>();
    for (CVE cve : cves) {
      if (cve.versions().contains(versionIdentifier)) {
        cvesOfVersion.add(cve);
      }
    }
    return cvesOfVersion;
  }

  public VersionIdentifier findSafestLatestVersion() {
    int lowestNumberOfCVE = toolSecurity.findCVEs(allVersions.getFirst()).size();
    VersionIdentifier safestLatestVersion = allVersions.getFirst();
    for (VersionIdentifier versionIdentifier : allVersions) {
      if (toolSecurity.findCVEs(versionIdentifier).size() < lowestNumberOfCVE) {
        lowestNumberOfCVE = toolSecurity.findCVEs(versionIdentifier).size();
        safestLatestVersion = versionIdentifier;
      }
    }
    return safestLatestVersion;
  }

  public VersionIdentifier findSafestNearestVersion() {
    int lowestNumberOfCVE = toolSecurity.findCVEs(version).size();
    VersionIdentifier safestNearestVersion = version;
    int upIndex = allVersions.indexOf(version);
    int downIndex = upIndex;
    while (upIndex != 0 || downIndex != allVersions.size() - 1) {
      if (upIndex > 0) {
        upIndex -= 1;
      }
      if (downIndex < allVersions.size() - 1) {
        downIndex += 1;
      }
      if (toolSecurity.findCVEs(allVersions.get(upIndex)).size() < lowestNumberOfCVE) {
        lowestNumberOfCVE = toolSecurity.findCVEs(allVersions.get(upIndex)).size();
        safestNearestVersion = allVersions.get(upIndex);
      } else if (toolSecurity.findCVEs(allVersions.get(downIndex)).size() < lowestNumberOfCVE) {
        lowestNumberOfCVE = toolSecurity.findCVEs(allVersions.get(downIndex)).size();
        safestNearestVersion = allVersions.get(downIndex);
      }
    }
    return safestNearestVersion;
  }

}
