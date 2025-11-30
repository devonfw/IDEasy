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
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Scans the IDEasy URL repository for tools, editions, and versions, and checks for known vulnerabilities using the OWASP Dependency-Check engine.
 * <p>For each tool and edition, vulnerabilities are collected and written to the corresponding {@link UrlSecurityFile}. Only vulnerabilities above a
 * configurable severity threshold are included</p>
 * <p>Note: Running this class may take a long time due to OWASP database updates.</p>
 * <p> For usage, see the {@link #main(String[]) main method}</p>
 */
public class BuildSecurityJsonFiles implements Runnable {

  //Evtl. Suppression.xml für False Positives: https://jeremylong.github.io/DependencyCheck/general/suppression.html

  private static final Set<Class<? extends AbstractAnalyzer>> ANALYZERS_TO_IGNORE = Set.of(ArchiveAnalyzer.class,
      RubyBundlerAnalyzer.class, FileNameAnalyzer.class, JarAnalyzer.class, CentralAnalyzer.class, NexusAnalyzer.class,
      ArtifactoryAnalyzer.class, NuspecAnalyzer.class, NugetconfAnalyzer.class, MSBuildProjectAnalyzer.class,
      AssemblyAnalyzer.class, OpenSSLAnalyzer.class, NodePackageAnalyzer.class, LibmanAnalyzer.class,
      NodeAuditAnalyzer.class, YarnAuditAnalyzer.class, PnpmAuditAnalyzer.class, RetireJsAnalyzer.class,
      FalsePositiveAnalyzer.class);

  private static final BigDecimal MIN_V_2_SEVERITY = new BigDecimal("0.0");

  private static final BigDecimal MIN_V_3_SEVERITY = new BigDecimal("0.0");

  private static final Logger LOG = LoggerFactory.getLogger(BuildSecurityJsonFiles.class);

  private final Path urlsPath;

  private final UrlRepository urlRepository;

  private final UrlMetadata urlMetadata;

  private final UpdateManager updateManager;

  private BuildSecurityJsonFiles(Path urlsPath) {

    super();
    this.urlsPath = urlsPath;
    this.urlRepository = UrlRepository.load(urlsPath);
    this.urlMetadata = new UrlMetadata(new IdeContextConsole(IdeLogLevel.INFO, null, false));
    UrlFinalReport report = new UrlFinalReport();
    this.updateManager = new UpdateManager(urlsPath, report, Instant.now());
  }

  @Override
  public void run() {
    List<Dependency> dependencies = findDependenciesWithVulnerabilities();
    processDependenciesWithVulnerabilities(dependencies);
  }


  /**
   * Main entry point for building security JSON files. Loads the URL repository, retrieves dependencies with vulnerabilities, and processes them to
   * generate/update security metadata.
   *
   * @param args command-line arguments; expects the first argument to be the path to the ide-urls repository.
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: " + BuildSecurityJsonFiles.class.getSimpleName() + " <path-to-ide-urls>");
      System.exit(1);
    }
    Path urlsPath = Path.of(args[0]);
    new BuildSecurityJsonFiles(urlsPath).run();
  }

  /**
   * Processes a list of vulnerable dependencies by updating the corresponding {@link UrlSecurityFile}s. Each vulnerability is mapped to the correct tool and
   * edition, and saved accordingly.
   *
   * @param dependencies the list of vulnerable {@link Dependency} objects.
   */
  public void processDependenciesWithVulnerabilities(List<Dependency> dependencies) {
    Set<Pair<String, String>> foundToolsAndEditions = new HashSet<>();

    for (Dependency dependency : dependencies) {
      String filePath = dependency.getFilePath();
      Path parent = Paths.get(filePath).getParent();
      String tool = parent.getParent().getParent().getFileName().toString();
      String edition = parent.getParent().getFileName().toString();

      AbstractUrlUpdater urlUpdater = this.updateManager.retrieveUrlUpdater(tool, edition);
      if (urlUpdater == null) {
        continue;
      }

      UrlSecurityFile securityFile = this.urlMetadata.getEdition(tool, edition).getSecurityFile();
      saveSecurityFile(dependency, foundToolsAndEditions, tool, edition, securityFile, urlUpdater);
    }
    printAffectedVersions();
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
   */
  public void saveSecurityFile(Dependency dependency, Set<Pair<String, String>> foundToolsAndEditions, String tool, String edition,
      UrlSecurityFile securityFile, AbstractUrlUpdater urlUpdater) {
    boolean newlyAdded = foundToolsAndEditions.add(new Pair<>(tool, edition));
    if (newlyAdded) {
      securityFile.clearSecurityWarnings();
    }

    Map<String, String> cpeToUrlVersion = buildCpeToUrlVersionMap(tool, edition, urlUpdater);
    Set<Vulnerability> vulnerabilities = dependency.getVulnerabilities(true);
    for (Vulnerability vulnerability : vulnerabilities) {
      addVulnerabilityToSecurityFile(vulnerability, securityFile, cpeToUrlVersion, urlUpdater);
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
  private Map<String, String> buildCpeToUrlVersionMap(String tool, String edition, AbstractUrlUpdater urlUpdater) {

    List<String> sortedVersions = this.urlMetadata.getSortedVersions(tool, edition, null).stream()
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
   * @return the {@link Dependency dependencies} with associated {@link Vulnerability vulnerabilities}.
   */
  private List<Dependency> findDependenciesWithVulnerabilities() {

    Settings settings = new Settings();
    Engine engine = new Engine(settings);

    FileTypeAnalyzer urlAnalyzer = new UrlAnalyzer(this.updateManager);
    engine.getFileTypeAnalyzers().add(urlAnalyzer);
    engine.getAnalyzers(AnalysisPhase.INFORMATION_COLLECTION).add(urlAnalyzer);

    // remove all analyzers that are not needed
    engine.getMode().getPhases().forEach(
        phase -> engine.getAnalyzers(phase).removeIf(analyzer -> ANALYZERS_TO_IGNORE.contains(analyzer.getClass())));

    engine.scan(this.updateManager.getUrlRepository().getPath().toString());
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
   */

  public void addVulnerabilityToSecurityFile(Vulnerability vulnerability, UrlSecurityFile securityFile,
      Map<String, String> cpeToUrlVersion, AbstractUrlUpdater urlUpdater) {

    String cveName = vulnerability.getName();

    BigDecimal severity = getBigDecimalSeverity(vulnerability);
    if (severity == null) {
      return;
    }

    if (vulnerability.getCvssV3() != null) {
      if (severity.compareTo(MIN_V_3_SEVERITY) < 0) {
        return;
      }
    } else if (severity.compareTo(MIN_V_2_SEVERITY) < 0) {
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

    Cve cve = new Cve(cveName, severity.doubleValue(), List.of(versionRange));
    securityFile.addCve(cve);
  }

  /**
   * Determines the severity of the vulnerability.
   *
   * @param vulnerability the vulnerability determined by OWASP dependency check.
   * @return the {@link BigDecimal severity} of the vulnerability.
   */
  protected static BigDecimal getBigDecimalSeverity(Vulnerability vulnerability) {

    if (vulnerability.getCvssV2() == null && vulnerability.getCvssV3() == null) {
      LOG.warn("Vulnerability without severity found: {}", vulnerability.getName());
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
  private static String getUrlVersion(String cpeVersion, Map<String, String> cpeToUrlVersion, AbstractUrlUpdater urlUpdater) {

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
  private void printAffectedVersions() {

    for (File tool : this.urlsPath.toFile().listFiles()) {
      if (!Files.isDirectory(tool.toPath())) {
        continue;
      }
      for (File edition : tool.listFiles()) {
        if (!edition.isDirectory()) {
          continue;
        }
        List<VersionIdentifier> sortedVersions = this.urlMetadata.getSortedVersions(tool.getName(), edition.getName(), null);
        UrlSecurityFile securityJsonFile = this.urlMetadata.getEdition(tool.getName(), edition.getName())
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
