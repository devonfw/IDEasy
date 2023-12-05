package com.devonfw.tools.security;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
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

public class BuildSecurityJsonFile {

  private static final Logger logger = LoggerFactory.getLogger(BuildSecurityJsonFile.class);

  private static final String CVE_BASE_URL = "https://nvd.nist.gov/vuln/detail/";

  private static BigDecimal minV2Severity;

  private static BigDecimal minV3Severity;

  public static void main(String[] args) {

    if (args.length != 2) {
      throw new RuntimeException("Please provide 2 numbers: minV2Severity and minV3Severity");
    }
    try {
      minV2Severity = new BigDecimal(String.format(args[0]));
      minV3Severity = new BigDecimal(String.format(args[1]));
    } catch (NumberFormatException e) {
      throw new RuntimeException("These two args could not be parsed as BigDecimal");
    }
    run();

  }

  private static void run() {

    IdeContext ideContext = new IdeContextConsole(IdeLogLevel.INFO, null, false);
    UpdateManager updateManager = new UpdateManager(ideContext.getUrlsPath(), null);

    // TODO edit dependency check properties file to switch off analysers, this file is currently read only
    // TODO maybe this can be done in pom.xml
    // or simply remove it like FileNameAnalyzer was removed

    // note: settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_USE_CACHE, false);

    // TODO ~/.m2/repository/org/owasp/dependency-check-utils/8.4.2/data/7.0/odc.update.lock
    // why is this not in projects dir but in user dir?

    Dependency[] dependencies = getDependenciesWithVulnerabilities(updateManager);

    for (Dependency dependency : dependencies) {

      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();
      AbstractUrlUpdater urlUpdater = updateManager.getUrlUpdater(tool);

      UrlSecurityJsonFile securityFile = ideContext.getUrls().getEdition(tool, edition).getSecurityJsonFile();

      // TODO maybe instead of clear check cve name and add only if cve name is not already present
      // TODO if new min security is higher than the severity in the loaded file, then remove the old one?

      // TODO wenn dieses repo auch als nightly laufen soll, wo sollen dann  die min severity werte herkommen?
      securityFile.clearSecurityWarnings();

      List<VersionIdentifier> sortedVersions = ideContext.getUrls().getSortedVersions(tool, edition);
      List<VersionIdentifier> sortedCpeVersions = sortedVersions.stream().map(VersionIdentifier::toString)
          .map(urlUpdater::mapUrlVersionToCpeVersion).map(VersionIdentifier::of)
          .collect(Collectors.toCollection(ArrayList::new));

      Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
      for (Vulnerability vulnerability : vulnerabilities) {
        addVulnerabilityToSecurityFile(vulnerability, securityFile, sortedCpeVersions);
      }
      securityFile.save();
    }
  }

  private static Dependency[] getDependenciesWithVulnerabilities(UpdateManager updateManager) {

    Settings settings = new Settings();
    // Using "try with resource" or engine.close() at the end resulted in SEVERE warning by owasp
    Engine engine = new Engine(settings);
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

  private static void addVulnerabilityToSecurityFile(Vulnerability vulnerability, UrlSecurityJsonFile securityFile,
      List<VersionIdentifier> sortedVersions) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      throw new RuntimeException("Vulnerability without severity found: " + vulnerability.getName());
    }
    boolean hasV3Severity = vulnerability.getCvssV3() != null;
    double severityDouble = hasV3Severity
        ? vulnerability.getCvssV3().getBaseScore()
        : vulnerability.getCvssV2().getScore();
    String formatted = String.format(Locale.US, "%.1f", severityDouble);
    BigDecimal severity = new BigDecimal(formatted);
    String severityVersion = hasV3Severity ? "v3" : "v2";
    String cveName = vulnerability.getName();
    String description = vulnerability.getDescription();
    String nistUrl = CVE_BASE_URL + cveName;
    List<String> referenceUrls = vulnerability.getReferences().stream().map(Reference::getUrl)
        .collect(Collectors.toList());
    if (referenceUrls.isEmpty()) {
      referenceUrls.add("No references found, try searching for the CVE name (" + cveName + ") on the web.");
    }
    boolean toLowSeverity = hasV3Severity
        ? severity.compareTo(minV3Severity) < 0
        : severity.compareTo(minV2Severity) < 0;

    if (toLowSeverity) {
      return;
    }
    VersionRange versionRange = getVersionRangeFromVulnerability(sortedVersions, vulnerability);
    if (versionRange == null) {
      logger.info(
          "Vulnerability {} is not relevant because its affected versions have no overlap with the versions available "
              + "through IDEasy.", vulnerability.getName());
      return;
    }

    securityFile.addSecurityWarning(versionRange, severity, severityVersion, cveName, description, nistUrl,
        referenceUrls);

  }

  /***
   * From the vulnerability determine the {@link VersionRange versionRange} to which the vulnerability applies.
   *
   * @param sortedVersions sorted versions of the tool available through IDEasy. Must match the format of the versions
   * in the vulnerability. See {@link AbstractUrlUpdater#mapUrlVersionToCpeVersion(String)}.
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @return the {@link VersionRange versionRange} to which the vulnerability applies.
   */
  static VersionRange getVersionRangeFromVulnerability(List<VersionIdentifier> sortedVersions,
      Vulnerability vulnerability) {

    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    String vEndExcluding = matchedVulnerableSoftware.getVersionEndExcluding();
    String vEndIncluding = matchedVulnerableSoftware.getVersionEndIncluding();
    String vStartExcluding = matchedVulnerableSoftware.getVersionStartExcluding();
    String vStartIncluding = matchedVulnerableSoftware.getVersionStartIncluding();

    if (vEndExcluding == null && vEndIncluding == null && vStartExcluding == null && vStartIncluding == null) {
      return VersionRange.of(">");
    }

    return getVersionRangeFromInterval(sortedVersions, vStartExcluding, vStartIncluding, vEndIncluding, vEndExcluding);
  }

  /***
   * From the interval determine the {@link VersionRange versionRange} to which the vulnerability applies. Since the
   * versions as specified in the vulnerability might not be in the {@code sortedVersions} list, the {@link VersionRange}
   * is determined by finding the versions in the {@code sortedVersions} list that, when selected, cover all affected
   * versions correctly.
   */
  static VersionRange getVersionRangeFromInterval(List<VersionIdentifier> sortedVersions, String vStartExcluding,
      String vStartIncluding, String vEndIncluding, String vEndExcluding) {

    VersionIdentifier min = null;
    if (vStartExcluding != null) {
      min = findMinFromStartExcluding(sortedVersions, vStartExcluding);
      if (min == null) {
        return null;
      }
    } else if (vStartIncluding != null) {
      min = findMinFromStartIncluding(sortedVersions, vStartIncluding);
      if (min == null) {
        return null;
      }
    }

    VersionIdentifier max = null;
    if (vEndIncluding != null) {
      max = findMaxFromEndIncluding(sortedVersions, vEndIncluding);
      if (max == null) {
        return null;
      }
    } else if (vEndExcluding != null) {
      max = findMaxFromEndExcluding(sortedVersions, vEndExcluding);
      if (max == null) {
        return null;
      }
    }
    return new VersionRange(min, max);
  }

  private static VersionIdentifier findMinFromStartExcluding(List<VersionIdentifier> sortedVs, String vStartExcluding) {

    VersionIdentifier startExcl = VersionIdentifier.of(vStartExcluding);
    for (int i = sortedVs.size() - 1; i >= 0; i--) {
      VersionIdentifier version = sortedVs.get(i);
      if (version.isGreater(startExcl)) {
        return version;
      }
    }
    return null;
  }

  private static VersionIdentifier findMinFromStartIncluding(List<VersionIdentifier> sortedVs, String vStartIncluding) {

    VersionIdentifier startIncl = VersionIdentifier.of(vStartIncluding);
    for (int i = sortedVs.size() - 1; i >= 0; i--) {
      VersionIdentifier version = sortedVs.get(i);
      if (version.compareTo(startIncl) >= 0) {
        return version;
      }
    }
    return null;
  }

  private static VersionIdentifier findMaxFromEndIncluding(List<VersionIdentifier> sortedVs, String vEndIncluding) {

    VersionIdentifier endIncl = VersionIdentifier.of(vEndIncluding);
    for (VersionIdentifier version : sortedVs) {
      if (version.compareTo(endIncl) <= 0) {
        return version;
      }
    }
    return null;
  }

  private static VersionIdentifier findMaxFromEndExcluding(List<VersionIdentifier> sortedVs, String vEndExcluding) {

    VersionIdentifier endExl = VersionIdentifier.of(vEndExcluding);
    for (VersionIdentifier version : sortedVs) {
      if (version.isLess(endExl)) {
        return version;
      }
    }
    return null;
  }

}
