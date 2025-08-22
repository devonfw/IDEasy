package com.devonfw.tools.IDEasy.dev;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;
import com.devonfw.tools.ide.url.model.file.json.CVE;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * This class is used to build the {@link UrlSecurityFile} files for IDEasy. It scans the {@link AbstractIdeContext#getUrlsPath() ide-url} folder for all tools,
 * editions and versions and checks for vulnerabilities by using the OWASP package. You must pass two arguments to the main method: minV2Severity and
 * minV3Severity. V2 and V3 severity are differentiated, because they are often considered not comparable. Vulnerabilities with severity lower than these values
 * will not be added to the {@link UrlSecurityFile}. For this the {@link com.devonfw.tools.ide.url.model.file.UrlStatusFile#STATUS_JSON} must be present in the
 * {@link com.devonfw.tools.ide.url.model.folder.UrlVersion}. If a vulnerability is found, it is added to the {@link UrlSecurityFile} of the corresponding tool
 * and edition. The previous content of the file is overwritten. Sometimes when running this class is takes a long time to finish. This is because of the OWASP
 * package, which is updating the vulnerability database.
 */
public class BuildSecurityJsonFiles {

  //Evtl. Suppression.xml für False Positives: https://jeremylong.github.io/DependencyCheck/general/suppression.html

  private static final Set<Class<? extends AbstractAnalyzer>> ANALYZERS_TO_IGNORE = Set.of(ArchiveAnalyzer.class,
      RubyBundlerAnalyzer.class, FileNameAnalyzer.class, JarAnalyzer.class, CentralAnalyzer.class, NexusAnalyzer.class,
      ArtifactoryAnalyzer.class, NuspecAnalyzer.class, NugetconfAnalyzer.class, MSBuildProjectAnalyzer.class,
      AssemblyAnalyzer.class, OpenSSLAnalyzer.class, NodePackageAnalyzer.class, LibmanAnalyzer.class,
      NodeAuditAnalyzer.class, YarnAuditAnalyzer.class, PnpmAuditAnalyzer.class, RetireJsAnalyzer.class,
      FalsePositiveAnalyzer.class);

  private static BigDecimal minV2Severity = new BigDecimal("0.0");

  private static BigDecimal minV3Severity = new BigDecimal("0.0");

  private static final Logger LOG = LoggerFactory.getLogger(BuildSecurityJsonFiles.class);

  /**
   * Main entry point for building security JSON files. Loads the URL repository, retrieves dependencies with vulnerabilities, and processes them to
   * generate/update security metadata.
   *
   * @param args command-line arguments; expects the first argument to be the path to the ide-urls repository.
   */

  public static void main(String[] args) {
    Path urlsPath = Path.of(args[0]);
    UrlRepository urlRepository = UrlRepository.load(urlsPath);
    UrlFinalReport report = new UrlFinalReport();
    UpdateManager updateManager = new UpdateManager(urlsPath, report, Instant.now());
    List<Dependency> dependencies = loadDependenciesWithVulnerabilities(updateManager);
    processDependenciesWithVulnerabilities(dependencies, updateManager, new UrlMetadata(new IdeContextConsole(IdeLogLevel.INFO, null, false), urlRepository),
        urlsPath);
  }

  /**
   * Loads dependencies that contain known vulnerabilities.
   *
   * @param updateManager the {@link UpdateManager} used to access tool metadata.
   * @return a list of {@link Dependency} objects with associated vulnerabilities.
   */
  public static List<Dependency> loadDependenciesWithVulnerabilities(UpdateManager updateManager) {
    return getDependenciesWithVulnerabilities(updateManager);
  }

  /**
   * Processes a list of vulnerable dependencies by updating the corresponding {@link UrlSecurityFile}s. Each vulnerability is mapped to the correct tool and
   * edition, and saved accordingly.
   *
   * @param dependencies the list of vulnerable {@link Dependency} objects.
   * @param updateManager the {@link UpdateManager} used to retrieve tool updaters.
   * @param urlMetadata the {@link UrlMetadata} used to resolve tool and version information
   * @param urlsPath the path to the ide-urls repository.
   */
  public static void processDependenciesWithVulnerabilities(List<Dependency> dependencies, UpdateManager updateManager, UrlMetadata urlMetadata,
      Path urlsPath) {
    Set<Pair<String, String>> foundToolsAndEditions = new HashSet<>();

    for (Dependency dependency : dependencies) {
      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();

      AbstractUrlUpdater urlUpdater = updateManager.retrieveUrlUpdater(tool, edition);
      if (urlUpdater == null) {
        continue;
      }

      UrlSecurityFile securityFile = urlMetadata.getEdition(tool, edition).getSecurityFile();
      saveSecurityFile(dependency, foundToolsAndEditions, tool, edition, securityFile, urlUpdater, urlMetadata);
    }

    printAffectedVersions(urlMetadata, urlsPath);
  }

  /**
   * Saves vulnerability information to the corresponding {@link UrlSecurityFile}. Clears previous warnings if this is the first time the tool/edition is
   * processed.
   *
   * @param dependency the {@link Dependency} containing vulnerability data.
   * @param foundToolsAndEditions a set tracking which tool/edition pairs have already been
   * @param tool the name of the tool (e.g., "java").
   * @param edition the edition of the tool (e.g., "oracle").
   * @param securityFile the {@link UrlSecurityFile} to update.
   * @param urlUpdater the {@link AbstractUrlUpdater} for the tool/edition.
   * @param urlMetadata the {@link UrlMetadata} used to resolve version and edition information.
   */
  public static void saveSecurityFile(Dependency dependency, Set<Pair<String, String>> foundToolsAndEditions, String tool, String edition,
      UrlSecurityFile securityFile, AbstractUrlUpdater urlUpdater, UrlMetadata urlMetadata) {
    boolean newlyAdded = foundToolsAndEditions.add(new Pair<>(tool, edition));
    if (newlyAdded) {
      securityFile.clearSecurityWarnings();
    }

    Map<String, String> cpeToUrlVersion = buildCpeToUrlVersionMap(tool, edition, urlUpdater, urlMetadata);
    Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
    for (Vulnerability vulnerability : vulnerabilities) {
      addVulnerabilityToSecurityFile(vulnerability, securityFile, cpeToUrlVersion, urlUpdater, urlMetadata);
    }
    securityFile.save();
  }

  /**
   * Creates a {@link Map} from CPE Version to {@link UrlVersion#getName() Url Version} containing all versions provided by IDEasy for the given tool and
   * edition.
   *
   * @param tool the tool to get the {@link Map map} for.
   * @param edition the edition to get the {@link Map map} for.
   * @param urlUpdater the {@link AbstractUrlUpdater} of the tool to get {@link AbstractUrlUpdater#mapUrlVersionToCpeVersion(String) mapping functions}
   *     between CPE Version and {@link UrlVersion#getName() Url Version}.
   * @return the {@link Map} from CPE Version to {@link UrlVersion#getName() Url Version}.
   */
  private static Map<String, String> buildCpeToUrlVersionMap(String tool, String edition,
      AbstractUrlUpdater urlUpdater, UrlMetadata urlMetadata) {

    List<String> sortedVersions = urlMetadata.getSortedVersions(tool, edition, null).stream()
        .map(VersionIdentifier::toString).toList();

    List<String> sortedCpeVersions = sortedVersions.stream().map(urlUpdater::mapUrlVersionToCpeVersion)
        .toList();

    Map<String, String> cpeToUrlVersion = new HashMap<>();
    for (int i = 0; i < sortedCpeVersions.size(); i++) {
      cpeToUrlVersion.put(sortedCpeVersions.get(i), sortedVersions.get(i));
    }

    return cpeToUrlVersion;
  }

  /**
   * Uses the {@link Engine OWASP engine} to scan the {@link AbstractIdeContext#getUrlsPath() ide-url} folder for dependencies and then runs
   * {@link Engine#analyzeDependencies() analyzes} them to get the {@link Vulnerability vulnerabilities}.
   *
   * @param updateManager the {@link UpdateManager} to use to get the {@link AbstractUrlUpdater} of the tool to get CPE Vendor, CPE Product and CPE edition
   *     of the tool, as well as the {@link AbstractUrlUpdater#mapCpeVersionToUrlVersion(String) CPE naming of its version}
   * @return the {@link Dependency dependencies} with associated {@link Vulnerability vulnerabilities}.
   */
  private static List<Dependency> getDependenciesWithVulnerabilities(UpdateManager updateManager) {

    Settings settings = new Settings();
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
    Dependency[] dependencies = engine.getDependencies();
    // remove dependencies without vulnerabilities
    List<Dependency> dependenciesFiltered = Arrays.stream(dependencies)
        .filter(dependency -> !dependency.getVulnerabilities().isEmpty()).toList();
    engine.close();
    return dependenciesFiltered;
  }

  /**
   * @param vulnerability the {@link Vulnerability} determined by OWASP dependency check.
   * @param securityFile the {@link UrlSecurityFile} of the tool and edition to which the vulnerability applies.
   * @param cpeToUrlVersion a {@link Map} from CPE Version to {@link UrlVersion#getName() Url Version}.
   * @param urlUpdater the {@link AbstractUrlUpdater} of the tool to get maps between CPE Version and {@link UrlVersion#getName() Url Version} naming.
   * @param urlMetadata the {@link UrlMetadata} used to resolve version and edition information.
   */

  public static void addVulnerabilityToSecurityFile(Vulnerability vulnerability, UrlSecurityFile securityFile,
      Map<String, String> cpeToUrlVersion, AbstractUrlUpdater urlUpdater, UrlMetadata urlMetadata) {

    String cveName = vulnerability.getName();

    BigDecimal severity = getBigDecimalSeverity(vulnerability);
    if (severity == null) {
      return;
    }

    if (vulnerability.getCvssV3() != null) {
      if (severity.compareTo(minV3Severity) < 0) {
        return;
      }
    } else if (severity.compareTo(minV2Severity) < 0) {
      return;
    }

    VersionRange versionRange = getVersionRangeFromVulnerability(vulnerability, cpeToUrlVersion, urlUpdater);
    if (versionRange == null) {
      LOG.info(
          "Vulnerability {} seems to be irrelevant because its affected versions have no overlap with the versions "
              + "available through IDEasy. If you think the versions should match, see the methode "
              + "mapUrlVersionToCpeVersion() in the UrlUpdater of the tool.",
          vulnerability.getName());
      return;
    }

    CVE cve = new CVE(cveName, severity.doubleValue(), List.of(versionRange));
    securityFile.addCve(cve);
  }

  /**
   * Prints debug information about the vulnerability.
   *
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @param versionRange the {@link VersionRange} to which the vulnerability applies.
   * @param severity the severity of the vulnerability.
   * @param cveName the CVE name of the vulnerability.
   * @param description the description of the vulnerability.
   * @param nistUrl the NIST url of the vulnerability.
   * @param securityFile the {@link UrlSecurityFile} of the tool and edition to which the vulnerability is added.
   */
  private static void debugInfo(Vulnerability vulnerability, VersionRange versionRange, BigDecimal severity,
      String cveName, String description, String nistUrl, UrlSecurityFile securityFile, IdeContext context) {

    context.debug("Writing vulnerability {} to security file at {}.", cveName, securityFile.getPath());
    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();
    context.debug("Matched CPE: {}.", matchedVulnerableSoftware.toCpe23FS());
    // the fields of cpe2.3 are:
    // cpe2.3 part, vendor, product, version, update, edition, language, swEdition, targetSw, targetHw, other;
    context.debug("The severity of the vulnerability is {}.", severity);
    context.debug("The description of the vulnerability is {}.", description);
    context.debug("The NIST url of the vulnerability is {}.", nistUrl);
    context.debug(
        "The determined VersionRange is {}, given OWASPs versionStartExcluding {}, versionStartIncluding {}, "
            + "versionEndIncluding {}, versionEndExcluding {}, single Version {} ",
        versionRange, matchedVulnerableSoftware.getVersionStartExcluding(),
        matchedVulnerableSoftware.getVersionStartIncluding(), matchedVulnerableSoftware.getVersionEndIncluding(),
        matchedVulnerableSoftware.getVersionEndExcluding(), matchedVulnerableSoftware.getVersion());
  }

  /**
   * Determines the severity of the vulnerability.
   *
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @return the {@link BigDecimal severity} of the vulnerability.
   */
  protected static BigDecimal getBigDecimalSeverity(Vulnerability vulnerability) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      LOG.warn("Vulnerability without severity found: " + vulnerability.getName());
      return null;
    }
    double severityDouble;
    if (vulnerability.getCvssV3() != null) {
      severityDouble = vulnerability.getCvssV3().getCvssData().getBaseScore();
    } else {
      severityDouble = vulnerability.getCvssV2().getCvssData().getBaseScore();
    }
    return BigDecimal.valueOf(severityDouble);
  }

  /**
   * From the vulnerability determine the {@link VersionRange versionRange} to which the vulnerability applies.
   *
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @param cpeToUrlVersion a {@link Map} from CPE Version to {@link UrlVersion#getName() Url Version}.
   * @param urlUpdater the {@link AbstractUrlUpdater} of the tool to get mapping functions between CPE Version and *
   *     {@link UrlVersion#getName() Url Version}. This is used as backup if the {@code cpeToUrlVersion} does not * contain the CPE Version.
   * @return the {@link VersionRange versionRange} to which the vulnerability applies.
   */
  protected static VersionRange getVersionRangeFromVulnerability(Vulnerability vulnerability,
      Map<String, String> cpeToUrlVersion, AbstractUrlUpdater urlUpdater) {

    VulnerableSoftware matchedVulnerableSoftware = vulnerability.getMatchedVulnerableSoftware();

    String vStartExcluding = matchedVulnerableSoftware.getVersionStartExcluding();
    String vStartIncluding = matchedVulnerableSoftware.getVersionStartIncluding();
    String vEndExcluding = matchedVulnerableSoftware.getVersionEndExcluding();
    String vEndIncluding = matchedVulnerableSoftware.getVersionEndIncluding();
    String singleVersion = matchedVulnerableSoftware.getVersion();

    vStartExcluding = getUrlVersion(vStartExcluding, cpeToUrlVersion, urlUpdater);
    vStartIncluding = getUrlVersion(vStartIncluding, cpeToUrlVersion, urlUpdater);
    vEndExcluding = getUrlVersion(vEndExcluding, cpeToUrlVersion, urlUpdater);
    vEndIncluding = getUrlVersion(vEndIncluding, cpeToUrlVersion, urlUpdater);
    singleVersion = getUrlVersion(singleVersion, cpeToUrlVersion, urlUpdater);

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
   * Maps the CPE Version to the {@link UrlVersion#getName() Url Version}.
   *
   * @param cpeVersion the CPE Version to map.
   * @param cpeToUrlVersion a {@link Map} from CPE Version to {@link UrlVersion#getName() Url Version}.
   * @param urlUpdater the {@link AbstractUrlUpdater} of the tool to get mapping functions between CPE Version and {@link UrlVersion#getName() Url Version}.
   *     This is used as backup if the {@code cpeToUrlVersion} does not contain the CPE Version.
   * @return the {@link UrlVersion#getName() Url Version} of the CPE Version.
   */
  private static String getUrlVersion(String cpeVersion, Map<String, String> cpeToUrlVersion,
      AbstractUrlUpdater urlUpdater) {

    String urlVersion = null;
    if (cpeVersion != null) {
      if (cpeToUrlVersion != null && cpeToUrlVersion.containsKey(cpeVersion)) {
        urlVersion = cpeToUrlVersion.get(cpeVersion);
      } else {
        urlVersion = urlUpdater.mapCpeVersionToUrlVersion(cpeVersion);
      }
    }
    return urlVersion;
  }

  /**
   * Determines the {@link VersionRange} from the interval provided by OWASP.
   *
   * @param startIncluding The {@link String version} of the start of the interval, including this version.
   * @param startExcluding The {@link String version} of the start of the interval, excluding this version.
   * @param endExcluding The {@link String version} of the end of the interval, excluding this version.
   * @param endIncluding The {@link String version} of the end of the interval, including this version.
   * @param singleVersion If the OWASP vulnerability only affects a single version, this is the {@link String version}.
   * @return the {@link VersionRange}.
   * @throws IllegalStateException if all parameters are {@code null}.
   */
  public static VersionRange getVersionRangeFromInterval(String startIncluding, String startExcluding,
      String endExcluding, String endIncluding, String singleVersion) throws IllegalStateException {

    if (endExcluding == null && endIncluding == null && startExcluding == null && startIncluding == null) {
      if (singleVersion == null) {
        throw new IllegalStateException(
            "Vulnerability has no interval of affected versions or single affected version.");
      }
      VersionIdentifier singleAffectedVersion = VersionIdentifier.of(singleVersion);
      return VersionRange.of(singleAffectedVersion, singleAffectedVersion, BoundaryType.CLOSED);

    }

    boolean leftExclusive = startIncluding == null;
    boolean rightExclusive = endIncluding == null;

    VersionIdentifier min = null;
    if (startIncluding != null) {
      min = VersionIdentifier.of(startIncluding);
    } else if (startExcluding != null) {
      min = VersionIdentifier.of(startExcluding);
    }

    VersionIdentifier max = null;
    if (endIncluding != null) {
      max = VersionIdentifier.of(endIncluding);
    } else if (endExcluding != null) {
      max = VersionIdentifier.of(endExcluding);
    }

    return VersionRange.of(min, max, BoundaryType.of(leftExclusive, rightExclusive));
  }

  /**
   * Prints the affected versions of each tool and edition.
   */
  private static void printAffectedVersions(UrlMetadata urlMetadata, Path urlsPath) {

    for (File tool : urlsPath.toFile().listFiles()) {
      if (!Files.isDirectory(tool.toPath())) {
        continue;
      }
      for (File edition : tool.listFiles()) {
        if (!edition.isDirectory()) {
          continue;
        }
        List<VersionIdentifier> sortedVersions = urlMetadata.getSortedVersions(tool.getName(), edition.getName(), null);
        UrlSecurityFile securityJsonFile = urlMetadata.getEdition(tool.getName(), edition.getName())
            .getSecurityFile();
        VersionIdentifier min = null;
        for (int i = sortedVersions.size() - 1; i >= 0; i--) {
          VersionIdentifier version = sortedVersions.get(i);
          if (securityJsonFile.contains(version)) {
            if (min == null) {
              min = version;
            }

          } else {
            if (min != null) {
              LOG.info("Tool '{}' with edition '{}' and versions '{}' are affected by vulnerabilities.",
                  tool.getName(), edition.getName(), VersionRange.of(min, null, BoundaryType.of(false, true)));
              min = null;
            }
          }
        }
        if (min != null) {
          LOG.info("Tool '{}' with edition '{}' and versions '{}' are affected by vulnerabilities.",
              tool.getName(), edition.getName(), VersionRange.of(min, null, BoundaryType.of(false, true)));
        }
      }
    }
  }
}
