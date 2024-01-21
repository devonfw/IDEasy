package com.devonfw.tools.security;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.version.BoundaryType;
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
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.util.MapUtil;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

// TODO Doesn't yet work with versions defined like this /<tool>/<edition>/latest
// TODO Sometimes when running this class is takes a long time to finish. Maybe this is because of the OWASP package, which
// is updating the vulnerabilities. A dirty fix is to stop the program and restart it. 

/**
 * This class is used to build the {@link UrlSecurityJsonFile} files for IDEasy. It scans the
 * {@link AbstractIdeContext#getUrlsPath() ide-url} folder for all tools, editions and versions and checks for
 * vulnerabilities by using the OWASP package. For this the
 * {@link com.devonfw.tools.ide.url.model.file.UrlStatusFile#STATUS_JSON} must be present in the
 * {@link com.devonfw.tools.ide.url.model.folder.UrlVersion}. If a vulnerability is found, it is added to the
 * {@link UrlSecurityJsonFile} of the corresponding tool and edition. The previous content of the file is overwritten.
 */
public class BuildSecurityJsonFiles {

  private static final Logger logger = LoggerFactory.getLogger(BuildSecurityJsonFiles.class);

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

      List<String> sortedVersions = context.getUrls().getSortedVersions(tool, edition).stream()
          .map(VersionIdentifier::toString).toList();
      List<String> sortedCpeVersions = sortedVersions.stream().map(urlUpdater::mapUrlVersionToCpeVersion)
          .collect(Collectors.toList());
      Map<String, String> cpeToUrlVersion = MapUtil.createMapfromLists(sortedCpeVersions, sortedVersions);

      Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
      for (Vulnerability vulnerability : vulnerabilities) {
        addVulnerabilityToSecurityFile(vulnerability, securityFile, urlUpdater, cpeToUrlVersion);
      }
      securityFile.save();
    }
    actuallyIgnoredCves.forEach(cve -> context.info("Ignored CVE " + cve + " because it is listed in CVES_TO_IGNORE."));
    printAffectedVersions(context);
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
      AbstractUrlUpdater urlUpdater, Map<String, String> cpeToUrlVersion) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      // TODO if this ever happens, add a case that handles this
      throw new RuntimeException("Vulnerability without severity found: " + vulnerability.getName() + "\\n"
          + " Please contact https://github.com/devonfw/IDEasy and make a request to get this feature implemented.");
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
    boolean tooLowSeverity = hasV3Severity ? severity.compareTo(minV3Severity) < 0
        : severity.compareTo(minV2Severity) < 0;

    if (tooLowSeverity) {
      return;
    }
    VersionRange versionRange = getVersionRangeFromVulnerability(vulnerability, urlUpdater, cpeToUrlVersion);
    if (versionRange == null) {
      logger.info(
          "Vulnerability {} seems to be irrelevant because its affected versions have no overlap with the versions "
              + "available through IDEasy. If you think the versions should match, see the methode "
              + "mapUrlVersionToCpeVersion() in the UrlUpdater of the tool.",
          vulnerability.getName());
      return;
    }
    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    String matchedCpe = matchedVulnerableSoftware.toCpe23FS();
    // the fields of cpe2.3 are:
    // cpe2.3 part, vendor, product, version, update, edition, language, swEdition, targetSw, targetHw, other;
    String interval = String.format(
        "start excluding = %s, start including = %s, end including = %s, end excluding = %s, single version = %s, is "
            + "the interval provided by OWASP. Manually double check whether the VersionRange was correctly determined.",
        matchedVulnerableSoftware.getVersionStartExcluding(), matchedVulnerableSoftware.getVersionStartIncluding(),
        matchedVulnerableSoftware.getVersionEndIncluding(), matchedVulnerableSoftware.getVersionEndExcluding(),
        matchedVulnerableSoftware.getVersion());

    securityFile.addSecurityWarning(versionRange, matchedCpe, interval, severity, severityVersion, cveName, description,
        nistUrl, referenceUrls);

  }

  /**
   * From the vulnerability determine the {@link VersionRange versionRange} to which the vulnerability applies.
   *
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @param urlUpdater the {@link AbstractUrlUpdater} of the tool to get maps between CPE Version and
   *        {@link UrlVersion#getName() Url Version}.
   * @return the {@link VersionRange versionRange} to which the vulnerability applies.
   */
  static VersionRange getVersionRangeFromVulnerability(Vulnerability vulnerability, AbstractUrlUpdater urlUpdater,
      Map<String, String> cpeToUrlVersion) {

    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();

    String vStartExcluding = matchedVulnerableSoftware.getVersionStartExcluding();
    String vStartIncluding = matchedVulnerableSoftware.getVersionStartIncluding();
    String vEndExcluding = matchedVulnerableSoftware.getVersionEndExcluding();
    String vEndIncluding = matchedVulnerableSoftware.getVersionEndIncluding();
    String singleVersion = matchedVulnerableSoftware.getVersion();

    vStartExcluding = getUrlVersion(vStartExcluding, urlUpdater, cpeToUrlVersion);
    vStartIncluding = getUrlVersion(vStartIncluding, urlUpdater, cpeToUrlVersion);
    vEndExcluding = getUrlVersion(vEndExcluding, urlUpdater, cpeToUrlVersion);
    vEndIncluding = getUrlVersion(vEndIncluding, urlUpdater, cpeToUrlVersion);
    singleVersion = getUrlVersion(singleVersion, urlUpdater, cpeToUrlVersion);

    VersionRange affectedRange;
    try {
      affectedRange = getVersionRangeFromInterval(vStartIncluding, vStartExcluding, vEndExcluding, vEndIncluding,
          singleVersion);
    } catch (IllegalStateException e) {
      throw new IllegalStateException(
          "Getting the VersionRange for the vulnerability " + vulnerability.getName() + " failed.", e);
    }
    return affectedRange;
  }

  /**
   * TODO
   *
   * @param cpeVersion
   * @param urlUpdater
   * @param cpeToUrlVersion
   * @return
   */
  private static String getUrlVersion(String cpeVersion, AbstractUrlUpdater urlUpdater,
      Map<String, String> cpeToUrlVersion) {

    String urlVersion = null;
    if (cpeVersion != null) {
      if (cpeToUrlVersion.containsKey(cpeVersion)) {
        urlVersion = cpeToUrlVersion.get(cpeVersion);
      } else {
        urlVersion = urlUpdater.mapCpeVersionToUrlVersion(cpeVersion);
      }
    }
    return urlVersion;
  }

  public static VersionRange getVersionRangeFromInterval(String si, String se, String ee, String ei, String s)
      throws IllegalStateException {

    if (ee == null && ei == null && se == null && si == null) {
      if (s == null) {
        throw new IllegalStateException(
            "Vulnerability has no interval of affected versions or single affected version.");
      }
      VersionIdentifier singleAffectedVersion = VersionIdentifier.of(s);
      return new VersionRange(singleAffectedVersion, singleAffectedVersion, BoundaryType.CLOSED);
    }

    boolean leftExclusive = si == null;
    boolean rightExclusive = ei == null;

    VersionIdentifier min = null;
    if (si != null) {
      min = VersionIdentifier.of(si);
    } else if (se != null) {
      min = VersionIdentifier.of(se);
    }

    VersionIdentifier max = null;
    if (ei != null) {
      max = VersionIdentifier.of(ei);
    } else if (ee != null) {
      max = VersionIdentifier.of(ee);
    }

    return new VersionRange(min, max, BoundaryType.of(leftExclusive, rightExclusive));
  }

  private static void printAffectedVersions(IdeContext context) {

    Path urlsPath = context.getUrlsPath();
    for (File tool : urlsPath.toFile().listFiles()) {
      if (!Files.isDirectory(tool.toPath())) {
        continue;
      }
      for (File edition : tool.listFiles()) {
        if (!edition.isDirectory()) {
          continue;
        }
        List<VersionIdentifier> sortedVersions = context.getUrls().getSortedVersions(tool.getName(), edition.getName());
        UrlSecurityJsonFile securityJsonFile = context.getUrls().getEdition(tool.getName(), edition.getName())
            .getSecurityJsonFile();

        VersionIdentifier min = null;
        for (int i = sortedVersions.size() - 1; i >= 0; i--) {
          VersionIdentifier version = sortedVersions.get(i);
          if (securityJsonFile.contains(version)) {
            if (min == null) {
              min = version;
            }

          } else {
            if (min != null) {
              System.out.println("Tool " + tool.getName() + " with edition " + edition.getName() + " and versions "
                  + new VersionRange(min, version, BoundaryType.of(false, true)) + " are affected by vulnerabilities.");
              min = null;
            }
          }
        }
        if (min != null) {
          System.out.println("Tool " + tool.getName() + " with edition " + edition.getName() + " and versions "
              + new VersionRange(min, null, BoundaryType.of(false, true)) + " are affected by vulnerabilities.");
        }
      }
    }
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
