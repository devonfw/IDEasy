package com.devonfw.tools.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.url.model.file.SecurityEntry;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.FileNameAnalyzer;
import org.owasp.dependencycheck.analyzer.FileTypeAnalyzer;
import org.owasp.dependencycheck.dependency.*;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.utils.Settings;

public class Main {
  public static void main(String[] args) {
    // TODO edit dependency check properties file to switch off analysers, this file is currently read only
    // TODO maybe this can be done in pom.xml
    // or simply remove it like FileNameAnalyzer was removed

    // TODO: note settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_USE_CACHE, false);

    // TODO ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Settings settings = new Settings();
    Engine engine = new Engine(settings); // doesn't work with "try with resource"

    IdeContext ideContext = new IdeContextConsole(IdeLogLevel.INFO, null, false);
    UpdateManager updateManager = new UpdateManager(ideContext.getUrlsPath(), null);

    FileTypeAnalyzer myAnalyzer = new UrlAnalyzer(updateManager);
    engine.getFileTypeAnalyzers().add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION)
        .removeIf(analyzer -> analyzer instanceof FileNameAnalyzer);

    engine.scan("C:\\projects\\_ide\\myUrls");

    try {
      engine.analyzeDependencies();// needed for db stuff which is private
    } catch (ExceptionCollection e) {
      throw new RuntimeException(e);
    }
    float minV2Severity = 0.0f;
    float minV3Severity = 0.0f;

    for (Dependency dependency : engine.getDependencies()) {
      Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);

      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();
      List<VersionIdentifier> sortedVersions = ideContext.getUrls().getSortedVersions(tool, edition);

      // TODO read min levels from console or args[]
      // TODO list all vulnerabilities, so maybe description, all fields of cvssv3 and cvssv2, cve name, source,
      // url of vulnerabilityIds, and vulnerableSoftware
      // TODO take all vulnerabilities, or ask for another min level und update the numbers of vulnerabilities
      // TODO write vulnerabilities to file -> new format? that includes CVE name?

      //      List<SecurityEntry> foundVulnerabilities = new ArrayList<>();

      for (Vulnerability vulnerability : vulnerabilities) {

        if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
          throw new RuntimeException("Vulnerability without severity found: " + vulnerability.getName());
        }
        boolean hasV3Severity = vulnerability.getCvssV3() != null;
        double severity = hasV3Severity
            ? vulnerability.getCvssV3().getBaseScore()
            : vulnerability.getCvssV2().getScore();
        String severityVersion = hasV3Severity ? "v3" : "v2";
        String cveName = vulnerability.getName();
        String description = vulnerability.getDescription();

        boolean toLowSeverity = hasV3Severity ? severity < minV3Severity : severity < minV2Severity;
        if (toLowSeverity) {
          continue;
        }

        VersionRange versionRange = getVersionRangeFromVulnerability(sortedVersions, vulnerability);
        SecurityEntry securityEntry = new SecurityEntry(versionRange, severity, severityVersion, cveName, description,
            null);
        ObjectMapper mapper = JsonMapping.create();
        try {
          String jsonString = mapper.writeValueAsString(securityEntry);
          System.out.println(jsonString);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    }
    engine.close();
  }

  static VersionRange getVersionRangeFromInterval(List<VersionIdentifier> sortedVersions, String vStartExcluding, String vStartIncluding,
      String vEndIncluding, String vEndExcluding) {

    VersionIdentifier max = null;
    if (vEndIncluding != null) {
      max = VersionIdentifier.of(vEndIncluding); // this allows that max is not part of the available versions, this has no impact on the contains method but maybe confusing
    } else if (vEndExcluding != null) {
      VersionIdentifier end = VersionIdentifier.of(vEndExcluding);
      for (VersionIdentifier version : sortedVersions) {
        if (version.isLess(end)) {
          max = version;
          break;
        }
      }
    }

    VersionIdentifier min = null;
    if (vStartIncluding != null) {
      min = VersionIdentifier.of(vStartIncluding);
    } else if (vStartExcluding != null) {
      for (int i = sortedVersions.size() - 1; i >= 0; i--) {
        VersionIdentifier version = sortedVersions.get(i);
        if (version.isGreater(VersionIdentifier.of(vStartExcluding))) {
          min = version;
          break;
        }
      }
    }
    return new VersionRange(min, max);
  }

  static VersionRange getVersionRangeFromVulnerability(List<VersionIdentifier> sortedVersions,
      Vulnerability vulnerability) {

    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    String vEndExcluding = matchedVulnerableSoftware.getVersionEndExcluding();
    String vEndIncluding = matchedVulnerableSoftware.getVersionEndIncluding();
    String vStartExcluding = matchedVulnerableSoftware.getVersionStartExcluding();
    String vStartIncluding = matchedVulnerableSoftware.getVersionStartIncluding();

    if (vEndExcluding == null && vEndIncluding == null && vStartExcluding == null && vStartIncluding == null) {
      throw new RuntimeException("Vulnerability without version range found: " + vulnerability.getName());
    }

    return getVersionRangeFromInterval(sortedVersions, vStartExcluding, vStartIncluding, vEndIncluding,
        vEndExcluding);
  }
}

