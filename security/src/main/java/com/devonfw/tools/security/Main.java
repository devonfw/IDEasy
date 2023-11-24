package com.devonfw.tools.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.FileNameAnalyzer;
import org.owasp.dependencycheck.analyzer.FileTypeAnalyzer;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Reference;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityJsonFile;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String CVE_BASE_URL = "https://nvd.nist.gov/vuln/detail/";

  private static double minV2Severity;

  private static double minV3Severity;

  public static void main(String[] args) {

    if (args.length != 2) {
      throw new RuntimeException("Please provide 2 numbers: minV2Severity and minV3Severity");
    }
    try {
      minV2Severity = Double.parseDouble(args[0]);
      minV3Severity = Double.parseDouble(args[1]);
    } catch (NumberFormatException e) {
      throw new RuntimeException("These two args could not be parsed as double");
    }
    run(minV2Severity, minV3Severity);

  }

  private static void run(double minV2Severity, double minV3Severity) {

    IdeContext ideContext = new IdeContextConsole(IdeLogLevel.INFO, null, false);

    // TODO edit dependency check properties file to switch off analysers, this file is currently read only
    // TODO maybe this can be done in pom.xml
    // or simply remove it like FileNameAnalyzer was removed

    // note: settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_USE_CACHE, false);

    // TODO ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Dependency[] dependencies = getDependenciesWithVulnerabilities(ideContext);

    for (Dependency dependency : dependencies) {

      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();

      UrlSecurityJsonFile securityFile = ideContext.getUrls().getEdition(tool, edition).getSecurityJsonFile();
      securityFile.clearSecurityMatches();

      List<VersionIdentifier> sortedVersions = ideContext.getUrls().getSortedVersions(tool, edition);

      Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
      for (Vulnerability vulnerability : vulnerabilities) {
        addVulnerabilityToSecurityFile(vulnerability, securityFile, sortedVersions);
      }
      securityFile.save();
    }
  }

  private static void addVulnerabilityToSecurityFile(Vulnerability vulnerability, UrlSecurityJsonFile securityFile,
      List<VersionIdentifier> sortedVersions) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      throw new RuntimeException("Vulnerability without severity found: " + vulnerability.getName());
    }
    boolean hasV3Severity = vulnerability.getCvssV3() != null;
    double severity = hasV3Severity ? vulnerability.getCvssV3().getBaseScore() : vulnerability.getCvssV2().getScore();
    String severityVersion = hasV3Severity ? "v3" : "v2";
    String cveName = vulnerability.getName();
    String description = vulnerability.getDescription();
    String nistUrl = CVE_BASE_URL + cveName;
    List<String> referenceUrls = vulnerability.getReferences().stream().map(Reference::getUrl)
        .collect(Collectors.toList());
    if (referenceUrls.isEmpty()) {
      referenceUrls.add("No references found, try searching for the CVE name (" + cveName + ") on the web.");
    }
    boolean toLowSeverity = hasV3Severity ? severity < minV3Severity : severity < minV2Severity;
    if (toLowSeverity) {
      return;
    }
    VersionRange versionRange = getVersionRangeFromVulnerability(sortedVersions, vulnerability);
    if (versionRange == null) {
      logger.info(
          "Vulnerability {} is not relevant because its affected versions have no overlap with the versions available "
              + "through IDEasy.",
          vulnerability.getName());
      return;
    }

    securityFile.addSecurityMatch(versionRange, severity, severityVersion, cveName, description, nistUrl,
        referenceUrls);

  }

  private static Dependency[] getDependenciesWithVulnerabilities(IdeContext ideContext) {

    Settings settings = new Settings();
    // Using try with resource or engine.close at the end resulted in SEVERE warning by owasp
    Engine engine = new Engine(settings);
    UpdateManager updateManager = new UpdateManager(ideContext.getUrlsPath(), null);
    FileTypeAnalyzer myAnalyzer = new UrlAnalyzer(updateManager);
    engine.getFileTypeAnalyzers().add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(myAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).removeIf(analyze -> analyze instanceof FileNameAnalyzer);

    // engine.scan(ideContext.getUrlsPath().toString());
    engine.scan("C:\\projects\\_ide\\myUrls");

    try {
      engine.analyzeDependencies();
    } catch (ExceptionCollection e) {
      throw new RuntimeException(e);
    }
    return engine.getDependencies();
  }

  static VersionRange getVersionRangeFromInterval(List<VersionIdentifier> sortedVersions, String vStartExcluding,
      String vStartIncluding, String vEndIncluding, String vEndExcluding) {

    VersionIdentifier max = null;
    if (vEndIncluding != null) {
      max = VersionIdentifier.of(vEndIncluding); // this allows that max is not part of the available versions, this has
      // no impact on the contains method but maybe confusing
    } else if (vEndExcluding != null) {
      VersionIdentifier end = VersionIdentifier.of(vEndExcluding);
      for (VersionIdentifier version : sortedVersions) {
        if (version.isLess(end)) {

          // TODO here the version from the name in url dir is v.2.7.0 for example and end is 2.7.2 which should be
          // smaller but is not
          // sinvce the v is there, i either have to map the sorted versions and remove the v or add the v to "end"
          max = version;
          break;
        }
      }
      if (max == null) { // vEndExcluding is smaller or equal than all available versions -> this vulnerability is not
        // relevant and just leaving max to be null could result in a version range like ">" meaning all versions are
        // effected, which is wrong.
        return null;
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
      if (min == null) { // vStartExcluding is greater or equal than all available versions -> this vulnerability is not
        // relevant and just leaving min to be null could result in a version range like ">" meaning all versions are
        // effected, which is wrong.
        return null;
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
      // maybe instead all versions are vulnerable in this case
      return VersionRange.of(">");
      // throw new RuntimeException("Vulnerability without version range found: " + vulnerability.getName());
    }

    return getVersionRangeFromInterval(sortedVersions, vStartExcluding, vStartIncluding, vEndIncluding, vEndExcluding);
  }
}
