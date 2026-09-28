package com.devonfw.tools.IDEasy.dev;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

import us.springett.parsers.cpe.Cpe;
import us.springett.parsers.cpe.CpeBuilder;
import us.springett.parsers.cpe.values.Part;

/**
 * Scans the IDEasy URL repository for tools, editions, and versions, and checks for known vulnerabilities using the OWASP Dependency-Check engine.
 * <p>For each tool and edition, vulnerabilities are collected and written to the corresponding {@link UrlSecurityFile}. Only vulnerabilities above a
 * configurable severity threshold are included</p>
 * <p>Note: Running this class may take a long time due to OWASP database updates.</p>
 * <p> For usage, see the {@link #main(String[]) main method}</p>
 */
public class BuildSecurityJsonFiles implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(BuildSecurityJsonFiles.class);

  private static final BigDecimal MIN_V_2_SEVERITY = new BigDecimal("0.0");

  private static final BigDecimal MIN_V_3_SEVERITY = new BigDecimal("0.0");

  private static final Set<String> DEFAULT_EDITIONS = Set.of("community");

  private static final Set<String> IGNORED_VALUES = Set.of("*", "windows", "linux", "mac", "aws");

  private static final Set<String> IGNORED_VERSIONS = Set.of("*", "-");

  private final UrlMetadata urlMetadata;

  private final UpdateManager updateManager;

  private final Engine engine;

  private BuildSecurityJsonFiles(Path urlsPath) {

    super();
    UrlFinalReport report = new UrlFinalReport();
    this.updateManager = new UpdateManager(urlsPath, report, Instant.now());
    IdeContextConsole context = new IdeContextConsole();
    this.urlMetadata = new UrlMetadata(context, this.updateManager.getUrlRepository());
    Settings settings = new Settings();
    engine = new Engine(settings);
  }

  @Override
  public void run() {
    try {
      this.engine.analyzeDependencies();

      CveDB database = engine.getDatabase();

      for (AbstractUrlUpdater updater : this.updateManager.getUpdaters()) {
        String updaterName = updater.getClass().getSimpleName();
        String tool = updater.getTool();
        LOG.info("Processing {} for tool {}", updaterName, tool);
        List<Vulnerability> vulnerabilities = findVulnerabilities(database, updater);
        if ((vulnerabilities == null) || (vulnerabilities.isEmpty())) {
          LOG.info("No vulnerabilities found for {} with CPE {}:{}", updaterName, updater.getCpeRegistry().getPrimaryVendor(),
              updater.getCpeRegistry().getPrimaryProduct());
        } else {
          for (String edition : updater.getEditions()) {
            LOG.info("Processing edition {} for tool {}", edition, tool);
            UrlSecurityFile securityFile = this.urlMetadata.getEdition(tool, edition).getSecurityFile();
            securityFile.clearSecurityWarnings(); // pointless parsing of JSON causing waste
            for (Vulnerability vulnerability : vulnerabilities) {
              LOG.info("Processing vulnerability {} for tool {}", vulnerability.getName(), tool);
              Cve cve = toCve(vulnerability, edition, updater);
              if (cve != null) {
                securityFile.addCve(cve);
              }
            }
            securityFile.save();
          }
        }
      }
      this.engine.close();

    } catch (Throwable e) {
      LOG.error("Failed to build security json files", e);
    }
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

  private Cve toCve(Vulnerability vulnerability, String edition, AbstractUrlUpdater urlUpdater) {

    String cveName = vulnerability.getName();

    BigDecimal severity = getBigDecimalSeverity(vulnerability);
    if (severity == null) {
      return null;
    }

    if (vulnerability.getCvssV3() != null) {
      if (severity.compareTo(MIN_V_3_SEVERITY) < 0) {
        return null;
      }
    } else if (severity.compareTo(MIN_V_2_SEVERITY) < 0) {
      return null;
    }

    Map<String, List<VersionRange>> conditions = new TreeMap<>();
    List<VersionRange> versions = toVersions(vulnerability, edition, urlUpdater, conditions);
    if (versions.isEmpty() && conditions.isEmpty()) {
      return null;
    }
    return new Cve(cveName, severity.doubleValue(), versions, conditions);
  }

  static List<VersionRange> toVersions(Vulnerability vulnerability, String edition, AbstractUrlUpdater urlUpdater,
      Map<String, List<VersionRange>> conditions) {

    String id = vulnerability.getName();
    List<VersionRange> versions = new ArrayList<>();
    for (VulnerableSoftware range : vulnerability.getVulnerableSoftware()) {
      OsVersionRange match = toVersionRange(range, edition, urlUpdater, id);
      if (match != null) {
        if (match.os() == null) {
          Cve.mergeVersionRage(versions, match.range());
        } else {
          List<VersionRange> osVersions = conditions.computeIfAbsent(match.os().toString(), key -> new ArrayList<>());
          Cve.mergeVersionRage(osVersions, match.range());
        }
      }
    }
    return versions;
  }

  /**
   * A {@link VersionRange} of a {@link VulnerableSoftware} entry together with the {@link OperatingSystem} it is restricted to.
   *
   * @param range the affected {@link VersionRange}.
   * @param os the {@link OperatingSystem} the {@link #range()} is restricted to, or {@code null} if it applies to all operating systems.
   */
  record OsVersionRange(VersionRange range, OperatingSystem os) {
  }

  static OsVersionRange toVersionRange(VulnerableSoftware range, String edition, AbstractUrlUpdater urlUpdater, String id) {

    if (!urlUpdater.matchesCpe(range.getVendor(), range.getProduct())) {
      return null;
    }
    String cpeEdition = findCpeEdition(range);
    if (cpeEdition != null) {
      LOG.debug("Checking {} for CPE edition {} = {} (tool edition).", id, cpeEdition, edition);
      if (!isEditionMatching(cpeEdition, edition, urlUpdater)) {
        LOG.info("Ignoring {} of {} because CPE edition {} != {} (tool edition).", range, id, cpeEdition, edition);
        return null;
      }
    }
    OperatingSystem os = findOperatingSystem(range);
    String startIncluding = mapVersion(range.getVersionStartIncluding(), urlUpdater);
    String startExcluding = mapVersion(range.getVersionStartExcluding(), urlUpdater);
    String endIncluding = mapVersion(range.getVersionEndIncluding(), urlUpdater);
    String endExcluding = mapVersion(range.getVersionEndExcluding(), urlUpdater);
    String singleVersion = mapVersion(range.getVersion(), urlUpdater);
    if ((endExcluding == null) && (endIncluding == null) && (startExcluding == null) && (startIncluding == null)) {
      if ((singleVersion == null)) {
        LOG.error("Vulnerability {} has no interval of affected versions or single affected version.", id);
        return null;
      }
      VersionIdentifier singleAffectedVersion = VersionIdentifier.of(singleVersion);
      return new OsVersionRange(VersionRange.of(singleAffectedVersion, singleAffectedVersion, BoundaryType.CLOSED), os);
    } else {
      VersionIdentifier min;
      boolean leftExclusive;
      if (startIncluding != null) {
        assert (startExcluding == null);
        min = VersionIdentifier.of(startIncluding);
        leftExclusive = false;
      } else if (startExcluding != null) {
        min = VersionIdentifier.of(startExcluding);
        leftExclusive = true;
      } else {
        min = null;
        leftExclusive = true;
      }
      VersionIdentifier max;
      boolean rightExclusive;
      if (endIncluding != null) {
        assert (endExcluding == null);
        max = VersionIdentifier.of(endIncluding);
        rightExclusive = false;
      } else if (endExcluding != null) {
        max = VersionIdentifier.of(endExcluding);
        rightExclusive = true;
      } else {
        max = null;
        rightExclusive = true;
      }
      return new OsVersionRange(VersionRange.of(min, max, BoundaryType.of(leftExclusive, rightExclusive)), os);
    }
  }

  /**
   * @param cpe the {@link Cpe} to check.
   * @return the {@link OperatingSystem} the given {@link Cpe} is restricted to (as found in its edition, swEdition, or targetSw component), or
   *     {@code null} if the {@link Cpe} is not restricted to a specific operating system.
   */
  static OperatingSystem findOperatingSystem(Cpe cpe) {

    OperatingSystem os = OperatingSystem.of(cpe.getEdition());
    if (os == null) {
      os = OperatingSystem.of(cpe.getSwEdition());
    }
    if (os == null) {
      os = OperatingSystem.of(cpe.getTargetSw());
    }
    return os;
  }

  private static boolean isEditionMatching(String cpeEdition, String urlEdition, AbstractUrlUpdater urlUpdater) {
    if (cpeEdition.equals(urlEdition)) {
      return true;
    }
    if (urlUpdater.getTool().equals(urlEdition)) {
      return DEFAULT_EDITIONS.contains(cpeEdition);
    }
    return false;
  }

  private static String mapVersion(String cveVersion, AbstractUrlUpdater urlUpdater) {

    if ((cveVersion == null) || IGNORED_VERSIONS.contains(cveVersion)) {
      return null;
    }
    return urlUpdater.mapVersion(cveVersion);
  }

  private static String findCpeEdition(Cpe cpe) {

    String cpeEdition = cpe.getEdition();
    if (isSpecificValue(cpeEdition)) {
      return cpeEdition;
    }
    cpeEdition = cpe.getSwEdition();
    if (isSpecificValue(cpeEdition)) {
      return cpeEdition;
    }
    cpeEdition = cpe.getTargetSw();
    if (isSpecificValue(cpeEdition)) {
      return cpeEdition;
    }
    return null;
  }

  private static boolean isSpecificValue(String value) {

    return (value != null) && !"*".equals(value) && !IGNORED_VALUES.contains(value);
  }

  private static List<Vulnerability> findVulnerabilities(CveDB database, AbstractUrlUpdater updater) {

    List<Cpe> searchCpes = createSearchCpes(updater);
    List<Vulnerability> vulnerabilities = new ArrayList<>();
    Set<String> seenNames = new LinkedHashSet<>();
    for (int i = 0; i < searchCpes.size(); i++) {
      Cpe cpe = searchCpes.get(i);
      List<Vulnerability> found = database.getVulnerabilities(cpe);
      if ((found != null) && !found.isEmpty()) {
        for (Vulnerability vulnerability : found) {
          if (seenNames.add(vulnerability.getName())) {
            vulnerabilities.add(vulnerability);
          }
        }
        if (i == 0) {
          return vulnerabilities;
        }
      }
    }
    return vulnerabilities;
  }

  private static List<Cpe> createSearchCpes(AbstractUrlUpdater updater) {

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();
    List<Cpe> searchCpes = new ArrayList<>();

    List<String> vendors = cpe.getVendors();
    List<String> products = cpe.getProducts();

    for (Part part : new Part[] { Part.APPLICATION, Part.OPERATING_SYSTEM }) {
      for (String vendor : vendors) {
        for (String product : products) {
          addSearchCpe(searchCpes, vendor, product, part);
        }
      }
    }
    return searchCpes;
  }

  private static void addSearchCpe(List<Cpe> searchCpes, String vendor, String product, Part part) {

    if ((vendor == null) || (product == null)) {
      return;
    }
    try {
      CpeBuilder cpeBuilder = new CpeBuilder();
      cpeBuilder.part(part);
      cpeBuilder.vendor(vendor);
      cpeBuilder.product(product);
      Cpe cpe = cpeBuilder.build();
      for (Cpe existing : searchCpes) {
        if (existing.getPart().equals(cpe.getPart()) && existing.getVendor().equals(cpe.getVendor()) && existing.getProduct().equals(cpe.getProduct())) {
          return;
        }
      }
      searchCpes.add(cpe);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create search CPE for vendor '" + vendor + "' and product '" + product + "'.", e);
    }
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

}
