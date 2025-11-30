package com.devonfw.tools.IDEasy.dev;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
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

  private final UrlMetadata urlMetadata;

  private final UpdateManager updateManager;

  private final Engine engine;

  private BuildSecurityJsonFiles(Path urlsPath) {

    super();
    UrlFinalReport report = new UrlFinalReport();
    this.updateManager = new UpdateManager(urlsPath, report, Instant.now());
    IdeContextConsole context = new IdeContextConsole(IdeLogLevel.INFO, null, false);
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
        String tool = updater.getTool();
        CpeBuilder cpeBuilder = new CpeBuilder();
        cpeBuilder.vendor(updater.getCpeVendor());
        cpeBuilder.product(updater.getCpeProduct());
        Cpe cpe = cpeBuilder.build();
        List<Vulnerability> vulnerabilities = database.getVulnerabilities(cpe);
        if ((vulnerabilities == null) || (vulnerabilities.isEmpty())) {
          LOG.info("No vulnerabilities found for {} with CPE {}", updater.getClass().getSimpleName(), cpe);
        } else {
          for (String edition : updater.getEditions()) {
            UrlSecurityFile securityFile = this.urlMetadata.getEdition(tool, edition).getSecurityFile();
            securityFile.clearSecurityWarnings(); // pointless parsing of JSON causing waste
            for (Vulnerability vulnerability : vulnerabilities) {
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

    List<VersionRange> versions = toVersions(vulnerability, edition, urlUpdater);
    if (versions.isEmpty()) {
      return null;
    }
    return new Cve(cveName, severity.doubleValue(), versions);
  }

  private static List<VersionRange> toVersions(Vulnerability vulnerability, String edition, AbstractUrlUpdater urlUpdater) {

    String id = vulnerability.getName();
    List<VersionRange> versions = new ArrayList<>();
    for (VulnerableSoftware range : vulnerability.getVulnerableSoftware()) {
      VersionRange versionRange = toVersionRange(range, edition, urlUpdater, id);
      if (versionRange != null) {
        versions.add(versionRange);
      }
    }
    return versions;
  }

  private static VersionRange toVersionRange(VulnerableSoftware range, String edition, AbstractUrlUpdater urlUpdater, String id) {

    String cpeEdition = findCpeEdition(range);
    if (cpeEdition != null) {
      LOG.debug("Checking {} for CPE edition {} = {} (tool edition).", id, cpeEdition, edition);
      if (!isEditionMatching(cpeEdition, edition, urlUpdater)) {
        LOG.info("Ignoring {} of {} because CPE edition {} != {} (tool edition).", range, id, cpeEdition, edition);
        return null;
      }
    }
    String startIncluding = mapVersion(range.getVersionStartIncluding(), urlUpdater);
    String startExcluding = mapVersion(range.getVersionStartExcluding(), urlUpdater);
    String endIncluding = mapVersion(range.getVersionEndIncluding(), urlUpdater);
    String endExcluding = mapVersion(range.getVersionEndExcluding(), urlUpdater);
    String singleVersion = mapVersion(range.getVersion(), urlUpdater);
    if ((endExcluding == null) && (endIncluding == null) && (startExcluding == null) && (startIncluding == null)) {
      if (singleVersion == null) {
        LOG.error("Vulnerability {} has no interval of affected versions or single affected version.", id);
        return null;
      }
      VersionIdentifier singleAffectedVersion = VersionIdentifier.of(singleVersion);
      return VersionRange.of(singleAffectedVersion, singleAffectedVersion, BoundaryType.CLOSED);
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
      return VersionRange.of(min, max, BoundaryType.of(leftExclusive, rightExclusive));
    }
  }

  private static boolean isEditionMatching(String cpeEdition, String urlEdition, AbstractUrlUpdater urlUpdater) {
    if (cpeEdition.equals(urlEdition)) {
      return true;
    }
    if (urlUpdater.getTool().equals(urlEdition)) {
      if (DEFAULT_EDITIONS.contains(cpeEdition)) {
        return true;
      }
    }
    return false;
  }

  private static String mapVersion(String cveVersion, AbstractUrlUpdater urlUpdater) {

    if (cveVersion == null) {
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
