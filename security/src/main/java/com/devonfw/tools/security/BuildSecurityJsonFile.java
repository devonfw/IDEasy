package com.devonfw.tools.security;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AbstractAnalyzer;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.ArchiveAnalyzer;
import org.owasp.dependencycheck.analyzer.ArtifactoryAnalyzer;
import org.owasp.dependencycheck.analyzer.AssemblyAnalyzer;
import org.owasp.dependencycheck.analyzer.CentralAnalyzer;
import org.owasp.dependencycheck.analyzer.FalsePositiveAnalyzer;
import org.owasp.dependencycheck.analyzer.FileNameAnalyzer;
import org.owasp.dependencycheck.analyzer.FileTypeAnalyzer;
import org.owasp.dependencycheck.analyzer.JarAnalyzer;
import org.owasp.dependencycheck.analyzer.LibmanAnalyzer;
import org.owasp.dependencycheck.analyzer.MSBuildProjectAnalyzer;
import org.owasp.dependencycheck.analyzer.NexusAnalyzer;
import org.owasp.dependencycheck.analyzer.NodeAuditAnalyzer;
import org.owasp.dependencycheck.analyzer.NodePackageAnalyzer;
import org.owasp.dependencycheck.analyzer.NugetconfAnalyzer;
import org.owasp.dependencycheck.analyzer.NuspecAnalyzer;
import org.owasp.dependencycheck.analyzer.OpenSSLAnalyzer;
import org.owasp.dependencycheck.analyzer.PnpmAuditAnalyzer;
import org.owasp.dependencycheck.analyzer.RetireJsAnalyzer;
import org.owasp.dependencycheck.analyzer.RubyBundlerAnalyzer;
import org.owasp.dependencycheck.analyzer.YarnAuditAnalyzer;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Reference;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.utils.Pair;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.url.model.file.UrlSecurityJsonFile;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

// TODO Doesn't yet work with versions defined like this /<tool>/<edition>/latest

/***
 * This class is used to build the {@link UrlSecurityJsonFile} file for IDEasy. It scans the
 * {@link AbstractIdeContext#getUrlsPath() ide-url} folder for all tools, editions and versions and checks for
 * vulnerabilities by using the OWASP package. For this the
 * {@link com.devonfw.tools.ide.url.model.file.UrlStatusFile#STATUS_JSON} must be present in the
 * {@link com.devonfw.tools.ide.url.model.folder.UrlVersion}. If a vulnerability is found, it is added to the
 * {@link UrlSecurityJsonFile} of the corresponding tool and edition. The previous content of the file is overwritten.
 */
public class BuildSecurityJsonFile {

  private static final Logger logger = LoggerFactory.getLogger(BuildSecurityJsonFile.class);

  private static final String CVE_BASE_URL = "https://nvd.nist.gov/vuln/detail/";

  private static final Set<String> CVES_TO_IGNORE = new HashSet<>();

  private static final Set<Class<? extends AbstractAnalyzer>> ANALYZERS_TO_IGNORE = Set.of(ArchiveAnalyzer.class,
      RubyBundlerAnalyzer.class, FileNameAnalyzer.class, JarAnalyzer.class, CentralAnalyzer.class, NexusAnalyzer.class,
      ArtifactoryAnalyzer.class, NuspecAnalyzer.class, NugetconfAnalyzer.class, MSBuildProjectAnalyzer.class,
      AssemblyAnalyzer.class, OpenSSLAnalyzer.class, NodePackageAnalyzer.class, LibmanAnalyzer.class,
      NodeAuditAnalyzer.class, YarnAuditAnalyzer.class, PnpmAuditAnalyzer.class, RetireJsAnalyzer.class,
      FalsePositiveAnalyzer.class);

  private static BigDecimal minV2Severity;

  private static BigDecimal minV3Severity;

  private static final Set<String> actuallyIgnoredCves = new HashSet<>();

  private static IdeContext context;

  /**
   * @param args Set {@code minV2Severity} with {@code args[0]} and {@code minV3Severity} with {@code args[1]}.
   *        Vulnerabilities with severity lower than these values will not be added to {@link UrlSecurityJsonFile}.
   */
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

    initCvesToIgnore();
    context = new IdeContextConsole(IdeLogLevel.INFO, null, false);
    UpdateManager updateManager = new UpdateManager(context.getUrlsPath(), null);

    Dependency[] dependencies = getDependenciesWithVulnerabilities(updateManager);
    Set<Pair<String, String>> foundToolsAndEditions = new HashSet<>();
    for (Dependency dependency : dependencies) {
      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();
      AbstractUrlUpdater urlUpdater = updateManager.getUrlUpdater(tool);

      UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool, edition).getSecurityJsonFile();
      boolean newlyAdded = foundToolsAndEditions.add(new Pair<>(tool, edition));
      if (newlyAdded) { // to assure that the file is cleared only once per tool and edition
        securityFile.clearSecurityWarnings();
      }

      List<VersionIdentifier> sortedVersions = context.getUrls().getSortedVersions(tool, edition);
      List<VersionIdentifier> sortedCpeVersions = sortedVersions.stream().map(VersionIdentifier::toString)
          .map(urlUpdater::mapUrlVersionToCpeVersion).map(VersionIdentifier::of)
          .collect(Collectors.toCollection(ArrayList::new));

      Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
      for (Vulnerability vulnerability : vulnerabilities) {
        addVulnerabilityToSecurityFile(vulnerability, securityFile, sortedVersions, sortedCpeVersions);
      }
      securityFile.save();
    }
    actuallyIgnoredCves.forEach(cve -> context.info("Ignored CVE " + cve + " because it is listed in CVES_TO_IGNORE."));
  }

  private static Dependency[] getDependenciesWithVulnerabilities(UpdateManager updateManager) {

    Settings settings = new Settings();
    // Using "try with resource" or engine.close() at the end resulted in SEVERE warning by OWASP.
    Engine engine = new Engine(settings);

    FileTypeAnalyzer urlAnalyzer = new UrlAnalyzer(updateManager);
    engine.getFileTypeAnalyzers().add(urlAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(urlAnalyzer);

    // remove all analyzers that are not needed
    engine.getMode().getPhases().forEach(
        phase -> engine.getAnalyzers(phase).removeIf(analyzer -> ANALYZERS_TO_IGNORE.contains(analyzer.getClass())));

    engine.scan(updateManager.getUrlRepository().getPath().toString());

    try {
      engine.analyzeDependencies();
    } catch (ExceptionCollection e) {
      throw new RuntimeException(e);
    }
    return engine.getDependencies();
  }

  private static void addVulnerabilityToSecurityFile(Vulnerability vulnerability, UrlSecurityJsonFile securityFile,
      List<VersionIdentifier> sortedVersions, List<VersionIdentifier> sortedCpeVersions) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      // if this ever happens, add a case that handles this
      throw new RuntimeException("Vulnerability without severity found: " + vulnerability.getName());
    }
    boolean hasV3Severity = vulnerability.getCvssV3() != null;
    double severityDouble = hasV3Severity ? vulnerability.getCvssV3().getBaseScore()
        : vulnerability.getCvssV2().getScore();
    String formatted = String.format(Locale.US, "%.1f", severityDouble);
    BigDecimal severity = new BigDecimal(formatted);
    String severityVersion = hasV3Severity ? "v3" : "v2";
    String cveName = vulnerability.getName();
    if (CVES_TO_IGNORE.contains(cveName)) {
      actuallyIgnoredCves.add(cveName);
      return;
    }

    String description = vulnerability.getDescription();
    String nistUrl = CVE_BASE_URL + cveName;
    List<String> referenceUrls = vulnerability.getReferences().stream().map(Reference::getUrl)
        .collect(Collectors.toList());
    if (referenceUrls.isEmpty()) {
      referenceUrls.add("No references found, try searching for the CVE name (" + cveName + ") on the web.");
    }
    boolean toLowSeverity = hasV3Severity ? severity.compareTo(minV3Severity) < 0
        : severity.compareTo(minV2Severity) < 0;

    if (toLowSeverity) {
      return;
    }
    VersionRange versionRange = getVersionRangeFromVulnerability(sortedVersions, sortedCpeVersions, vulnerability);
    if (versionRange == null) {
      logger.info(
          "Vulnerability {} seems to be irrelevant because its affected versions have no overlap with the versions "
              + "available through IDEasy. If you think the versions should match, see the methode "
              + "mapUrlVersionToCpeVersion() in the UrlUpdater of the tool.",
          vulnerability.getName());
      return;
    }
    // for manual detection of false positives: check of the interval of affected versions is correctly transformed to
    // the versionRange
    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    String matchedCpe = matchedVulnerableSoftware.toCpe23FS();
    // the fields of cpe2.3 are:
    // cpe2.3 part, vendor, product, version, update, edition, language, swEdition, targetSw, targetHw, other;
    String interval = String.format(" ( %s, [ %s, ] %s, ) %s", matchedVulnerableSoftware.getVersionStartExcluding(),
        matchedVulnerableSoftware.getVersionStartIncluding(), matchedVulnerableSoftware.getVersionEndIncluding(),
        matchedVulnerableSoftware.getVersionEndExcluding());

    securityFile.addSecurityWarning(versionRange, matchedCpe, interval, severity, severityVersion, cveName, description,
        nistUrl, referenceUrls);

  }

  /***
   * From the vulnerability determine the {@link VersionRange versionRange} to which the vulnerability applies.
   *
   * @param sortedVersions sorted versions of the tool available through IDEasy.
   * @param sortedCpeVersions sorted versions of the tool. Must match the format of the CPE versions. See
   *        {@link AbstractUrlUpdater#mapUrlVersionToCpeVersion(String)}.
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @return the {@link VersionRange versionRange} to which the vulnerability applies.
   */
  static VersionRange getVersionRangeFromVulnerability(List<VersionIdentifier> sortedVersions,
      List<VersionIdentifier> sortedCpeVersions, Vulnerability vulnerability) {

    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    String vEndExcluding = matchedVulnerableSoftware.getVersionEndExcluding();
    String vEndIncluding = matchedVulnerableSoftware.getVersionEndIncluding();
    String vStartExcluding = matchedVulnerableSoftware.getVersionStartExcluding();
    String vStartIncluding = matchedVulnerableSoftware.getVersionStartIncluding();

    if (vEndExcluding == null && vEndIncluding == null && vStartExcluding == null && vStartIncluding == null) {
      String singleAffectedVersion = vulnerability.getMatchedVulnerableSoftware().getVersion();
      return VersionRange.of(singleAffectedVersion + ">" + singleAffectedVersion);
    }

    return getVersionRangeFromInterval(sortedVersions, sortedCpeVersions, vStartExcluding, vStartIncluding,
        vEndIncluding, vEndExcluding);
  }

  static VersionRange getVersionRangeFromInterval(List<VersionIdentifier> sortedVersions, String vStartExcluding,
      String vStartIncluding, String vEndIncluding, String vEndExcluding) {

    return getVersionRangeFromInterval(sortedVersions, sortedVersions, vStartExcluding, vStartIncluding, vEndIncluding,
        vEndExcluding);
  }

  /***
   * From the interval determine the {@link VersionRange versionRange} to which the vulnerability applies. Since the
   * versions as specified in the vulnerability might not be in the {@code sortedVersions} list, the
   * {@link VersionRange} is determined by finding the versions in the {@code sortedVersions} list that, when selected,
   * cover all affected versions correctly.
   */
  static VersionRange getVersionRangeFromInterval(List<VersionIdentifier> sortedVersions,
      List<VersionIdentifier> sortedCpeVersions, String vStartExcluding, String vStartIncluding, String vEndIncluding,
      String vEndExcluding) {

    VersionIdentifier min = null;
    if (vStartExcluding != null) {
      min = findMinFromStartExcluding(sortedVersions, sortedCpeVersions, vStartExcluding);
      if (min == null) {
        return null;
      }
    } else if (vStartIncluding != null) {
      min = findMinFromStartIncluding(sortedVersions, sortedCpeVersions, vStartIncluding);
      if (min == null) {
        return null;
      }
    }

    VersionIdentifier max = null;
    if (vEndIncluding != null) {
      max = findMaxFromEndIncluding(sortedVersions, sortedCpeVersions, vEndIncluding);
      if (max == null) {
        return null;
      }
    } else if (vEndExcluding != null) {
      max = findMaxFromEndExcluding(sortedVersions, sortedCpeVersions, vEndExcluding);
      if (max == null) {
        return null;
      }
    }
    return new VersionRange(min, max);
  }

  private static VersionIdentifier findMinFromStartExcluding(List<VersionIdentifier> sortedVs,
      List<VersionIdentifier> sortedCpeVs, String vStartExcluding) {

    VersionIdentifier startExcl = VersionIdentifier.of(vStartExcluding);
    for (int i = sortedCpeVs.size() - 1; i >= 0; i--) {
      VersionIdentifier version = sortedCpeVs.get(i);
      if (version.isGreater(startExcl) && !version.compareVersion(startExcl).isUnsafe()) {
        return sortedVs.get(i);
      }
    }
    return null;
  }

  private static VersionIdentifier findMinFromStartIncluding(List<VersionIdentifier> sortedVs,
      List<VersionIdentifier> sortedCpeVs, String vStartIncluding) {

    VersionIdentifier startIncl = VersionIdentifier.of(vStartIncluding);
    for (int i = sortedCpeVs.size() - 1; i >= 0; i--) {
      VersionIdentifier version = sortedCpeVs.get(i);
      if (version.compareTo(startIncl) >= 0) {
        return sortedVs.get(i);
      }
    }
    return null;
  }

  private static VersionIdentifier findMaxFromEndIncluding(List<VersionIdentifier> sortedVs,
      List<VersionIdentifier> sortedCpeVs, String vEndIncluding) {

    VersionIdentifier endIncl = VersionIdentifier.of(vEndIncluding);
    for (int i = 0; i < sortedCpeVs.size(); i++) {
      VersionIdentifier version = sortedCpeVs.get(i);
      if (version.compareTo(endIncl) <= 0) {
        return sortedVs.get(i);
      }
    }
    return null;
  }

  private static VersionIdentifier findMaxFromEndExcluding(List<VersionIdentifier> sortedVs,
      List<VersionIdentifier> sortedCpeVs, String vEndExcluding) {

    VersionIdentifier endExl = VersionIdentifier.of(vEndExcluding);
    for (int i = 0; i < sortedCpeVs.size(); i++) {
      VersionIdentifier version = sortedCpeVs.get(i);
      if (version.isLess(endExl)) {
        return sortedVs.get(i);
      }
    }
    return null;
  }

  private static void printAllOwaspAnalyzers(Engine engine) {

    engine.getMode().getPhases().forEach(phase -> engine.getAnalyzers(phase)
        .forEach(analyzer -> System.out.println("Phase: " + phase + ", Analyzer: " + analyzer.getName())));
  }

  private static void initCvesToIgnore() {

    if (CVES_TO_IGNORE.isEmpty()) {
      // ......................................vendor......product......why was is ignored
      CVES_TO_IGNORE.add("CVE-2021-36230"); // hashicorp...terraform....https://github.com/anchore/grype/issues/1377
    }
  }

}
