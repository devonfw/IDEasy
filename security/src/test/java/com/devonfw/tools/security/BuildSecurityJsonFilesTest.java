package com.devonfw.tools.security;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.tool.mvn.MvnUrlUpdater;
import com.devonfw.tools.ide.url.model.file.UrlSecurityJsonFile;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import io.github.jeremylong.openvulnerability.client.nvd.CvssV2;
import io.github.jeremylong.openvulnerability.client.nvd.CvssV2Data;
import io.github.jeremylong.openvulnerability.client.nvd.CvssV3;
import io.github.jeremylong.openvulnerability.client.nvd.CvssV3Data;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import us.springett.parsers.cpe.exceptions.CpeValidationException;
import us.springett.parsers.cpe.values.Part;

import static com.devonfw.tools.security.BuildSecurityJsonFiles.addVulnerabilityToSecurityFile;
import static com.devonfw.tools.security.BuildSecurityJsonFiles.getVersionRangeFromInterval;

/** Test of {@link BuildSecurityJsonFiles}. */
public class BuildSecurityJsonFilesTest extends Assertions {

  /** Test of {@link BuildSecurityJsonFiles#getVersionRangeFromInterval(String, String, String, String, String)}. */
  @Test
  public void testGetVersionRangeFromInterval() {

    // act & assert
    assertThat(getVersionRangeFromInterval("1", null, null, null, null)).isEqualTo(VersionRange.of("[1,)"));
    assertThat(getVersionRangeFromInterval(null, "1", null, null, null)).isEqualTo(VersionRange.of("(1,)"));
    assertThat(getVersionRangeFromInterval(null, null, "1", null, null)).isEqualTo(VersionRange.of("(,1)"));
    assertThat(getVersionRangeFromInterval(null, null, null, "1", null)).isEqualTo(VersionRange.of("(,1]"));
    assertThat(getVersionRangeFromInterval(null, null, null, null, "1")).isEqualTo(VersionRange.of("[1,1]"));
    assertThat(getVersionRangeFromInterval(null, "1", null, "2", "1")).isEqualTo(VersionRange.of("(1,2]"));
  }

  /**
   * Test of {@link BuildSecurityJsonFiles#getBigDecimalSeverity(Vulnerability)}.
   * 
   * @throws CpeValidationException if the CPE is invalid. Should never happen since it is hard coded.
   */
  @Test
  public void testGetBigDecimalSeverity() throws CpeValidationException {

    // arrange
    Vulnerability vulnerabilityV2 = getTestVulnerability(2.2, false, null, null, null, null, "1.0.0");
    Vulnerability vulnerabilityV3 = getTestVulnerability(3.0, true, null, null, null, null, "1.0.0");

    // act & assert
    assertThat(BuildSecurityJsonFiles.getBigDecimalSeverity(vulnerabilityV2)).isEqualTo(BigDecimal.valueOf(2.2));
    assertThat(BuildSecurityJsonFiles.getBigDecimalSeverity(vulnerabilityV3)).isEqualTo(BigDecimal.valueOf(3.0));
  }

  /**
   * Test of
   * {@link BuildSecurityJsonFiles#addVulnerabilityToSecurityFile(Vulnerability, UrlSecurityJsonFile, Map, AbstractUrlUpdater)}
   * if the vulnerability affects a single version. Also tests the case where the bool {@code isV3Severity} is set to
   * {@code true}.
   * 
   * @throws CpeValidationException if the CPE is invalid. Should never happen since it is hard coded.
   */
  @Test
  public void testAddVulnerabilityToSecurityFileSingleVersion() throws CpeValidationException {

    // arrange
    String affectedVersion = "1.0.0";
    Vulnerability vulnerability = getTestVulnerability(3.0, false, null, null, null, null, affectedVersion);
    AbstractUrlUpdater updater = new MvnUrlUpdater();
    UrlSecurityJsonFile securityJsonFile = getTestUrlSecurityJsonFile();

    // act
    addVulnerabilityToSecurityFile(vulnerability, securityJsonFile, null, updater);
    Set<UrlSecurityWarning> warnings = securityJsonFile
        .getMatchingSecurityWarnings(VersionIdentifier.of(affectedVersion));

    // assert
    assertThat(warnings).hasSize(1);
    UrlSecurityWarning warning = warnings.iterator().next();
    assertThat(warning.getVersionRange()).isEqualTo(new VersionRange(VersionIdentifier.of(affectedVersion),
        VersionIdentifier.of(affectedVersion), BoundaryType.CLOSED));
    assertThat(warning.getCveName()).isEqualTo("testCveName");
    assertThat(warning.getSeverity()).isEqualTo(BigDecimal.valueOf(3.0));
    assertThat(warning.getDescription()).isEqualTo("testDescription");
    assertThat(warning.getNistUrl()).isEqualTo("https://nvd.nist.gov/vuln/detail/testCveName");
  }

  /**
   * Test of
   * {@link BuildSecurityJsonFiles#addVulnerabilityToSecurityFile(Vulnerability, UrlSecurityJsonFile, Map, AbstractUrlUpdater)}
   * when the vulnerability affects a range of versions. Also tests the case where the bool {@code isV3Severity} is set
   * to {@code true}.
   * 
   * @throws CpeValidationException if the CPE is invalid. Should never happen since it is hard coded.
   */
  @Test
  public void testAddVulnerabilityToSecurityFileInterval() throws CpeValidationException {

    // arrange
    Vulnerability vulnerability = getTestVulnerability(3.1, true, null, "3", "1", null, null);
    AbstractUrlUpdater updater = new MvnUrlUpdater();
    UrlSecurityJsonFile securityJsonFile = getTestUrlSecurityJsonFile();

    // act
    addVulnerabilityToSecurityFile(vulnerability, securityJsonFile, null, updater);
    Set<UrlSecurityWarning> warnings = securityJsonFile.getMatchingSecurityWarnings(VersionIdentifier.of("2"));

    // assert
    assertThat(warnings).hasSize(1);
    UrlSecurityWarning warning = warnings.iterator().next();
    assertThat(warning.getVersionRange())
        .isEqualTo(new VersionRange(VersionIdentifier.of("1"), VersionIdentifier.of("3"), BoundaryType.LEFT_OPEN));
    assertThat(warning.getCveName()).isEqualTo("testCveName");
    assertThat(warning.getSeverity()).isEqualTo(BigDecimal.valueOf(3.1));
    assertThat(warning.getDescription()).isEqualTo("testDescription");
    assertThat(warning.getNistUrl()).isEqualTo("https://nvd.nist.gov/vuln/detail/testCveName");
  }

  /**
   * Creates a {@link Vulnerability} with the given parameters. To be used in the {@link BuildSecurityJsonFilesTest
   * tests}.
   * 
   * @param severity the severity of the vulnerability.
   * @param isV3Severity whether the severity is a V3 severity or not.
   * @param versionEndExcluding if the vulnerability should affect a range of versions, the version that marks the end
   *        of the range, excluding this version.
   * @param versionEndIncluding if the vulnerability should affect a range of versions, the version that marks the end
   *        of the range, including this version.
   * @param versionStartExcluding if the vulnerability should affect a range of versions, the version that marks the
   *        start of the range, excluding this version.
   * @param versionStartIncluding if the vulnerability should affect a range of versions, the version that marks the
   *        start of the range, including this version.
   * @param version if the vulnerability should affect a single version, the version that is affected.
   * @return the created {@link Vulnerability}.
   * @throws CpeValidationException if the CPE is invalid. Should never happen since it is hard coded.
   */
  private Vulnerability getTestVulnerability(double severity, boolean isV3Severity, String versionEndExcluding,
      String versionEndIncluding, String versionStartExcluding, String versionStartIncluding, String version)
      throws CpeValidationException {

    Vulnerability vulnerability = new Vulnerability();
    if (!isV3Severity) {
      CvssV2Data data2 = new CvssV2Data("2.0", null, null, null, null, null, null, null, severity, null, null, null,
          null, null, null, null, null, null, null, null);
      CvssV2 cvssV2 = new CvssV2(null, null, data2, null, null, null, null, null, null, null, null);
      vulnerability.setCvssV2(cvssV2);
    } else {
      CvssV3Data data3 = new CvssV3Data(CvssV3Data.Version._3_1, null, null, null, null, null, null, null, null, null,
          severity, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null);
      CvssV3 cvssV3 = new CvssV3(null, null, data3, null, null);
      vulnerability.setCvssV3(cvssV3);
    }
    vulnerability.setName("testCveName");
    vulnerability.setDescription("testDescription");
    boolean vulnerable = true;
    if (version == null) {
      version = "*";
    }
    VulnerableSoftware matchedVulnerableSoftware = new VulnerableSoftware(Part.ANY, "vendor", "product", version,
        "update", "edition", "language", "softwareEdition", "targetSoftware", "targetHardware", "other",
        versionEndExcluding, versionEndIncluding, versionStartExcluding, versionStartIncluding, vulnerable);
    vulnerability.setMatchedVulnerableSoftware(matchedVulnerableSoftware);

    return vulnerability;
  }

  /**
   * Creates a {@link UrlSecurityJsonFile} for testing purposes using the files in resources.
   * 
   * @return the created {@link UrlSecurityJsonFile}.
   */
  private static UrlSecurityJsonFile getTestUrlSecurityJsonFile() {

    UrlRepository ulrRepo = new UrlRepository(Paths.get("src/test/resources/_ide/urls"));
    ulrRepo.load(true);
    UrlTool urlTool = ulrRepo.getOrCreateChild("testTool");
    UrlEdition urlEdition = urlTool.getOrCreateChild("testEdition");
    UrlSecurityJsonFile securityJsonFile = new UrlSecurityJsonFile(urlEdition);
    return securityJsonFile;
  }

}