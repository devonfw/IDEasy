package com.devonfw.tools.IDEasy.dev;

import java.util.Set;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.dependency.VulnerableSoftwareBuilder;

import com.devonfw.tools.IDEasy.dev.BuildSecurityJsonFiles.OsVersionRange;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.version.VersionRange;

import us.springett.parsers.cpe.exceptions.CpeValidationException;
import us.springett.parsers.cpe.values.Part;

/**
 * Test of {@link BuildSecurityJsonFiles}.
 */
class BuildSecurityJsonFilesTest extends Assertions {

  private static final String TOOL = "docker";

  private final TestCpeUpdater updater = new TestCpeUpdater();

  @Test
  void testFindOperatingSystemFromTargetSw() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.targetSw("windows"));

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isEqualTo(OperatingSystem.WINDOWS);
  }

  @Test
  void testFindOperatingSystemFromEdition() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.edition("linux"));

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isEqualTo(OperatingSystem.LINUX);
  }

  @Test
  void testFindOperatingSystemFromSwEdition() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.swEdition("mac"));

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isEqualTo(OperatingSystem.MAC);
  }

  @Test
  void testFindOperatingSystemReturnsNullForGenericCpe() throws CpeValidationException {

    VulnerableSoftware software = software(b -> {
    });

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isNull();
  }

  @Test
  void testFindOperatingSystemIgnoresNonOsValues() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.targetSw("aws"));

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isNull();
  }

  @Test
  void testFindOperatingSystemFromOsVariantsAndDistros() throws CpeValidationException {

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.targetSw("macos")))).isEqualTo(OperatingSystem.MAC);
    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.targetSw("mac_os_x")))).isEqualTo(OperatingSystem.MAC);
    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.targetSw("ubuntu_linux")))).isEqualTo(OperatingSystem.LINUX);
    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.targetSw("debian")))).isEqualTo(OperatingSystem.LINUX);
    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.targetSw("windows_server_2019")))).isEqualTo(OperatingSystem.WINDOWS);
  }

  @Test
  void testFindOperatingSystemFromEditionCombinedWithOs() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.edition("desktop_windows"));

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software)).isEqualTo(OperatingSystem.WINDOWS);
  }

  @Test
  void testFindOperatingSystemIgnoresValuesContainingOsKeywordOnlyAsPartOfWord() throws CpeValidationException {

    assertThat(BuildSecurityJsonFiles.findOperatingSystem(software(b -> b.edition("machine_learning")))).isNull();
  }

  @Test
  void testToVersionRangeDoesNotTreatCombinedOsEditionAsToolEdition() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.edition("desktop_windows").versionStartIncluding("1.0.0").versionEndExcluding("2.0.0"));

    OsVersionRange match = BuildSecurityJsonFiles.toVersionRange(software, TOOL, this.updater, "CVE-TEST");

    assertThat(match).isNotNull();
    assertThat(match.os()).isEqualTo(OperatingSystem.WINDOWS);
  }

  @Test
  void testToVersionRangeRoutesOsSpecificRangeToOs()throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.targetSw("windows").versionStartIncluding("1.0.0").versionEndExcluding("2.0.0"));

    OsVersionRange match = BuildSecurityJsonFiles.toVersionRange(software, TOOL, this.updater, "CVE-TEST");

    assertThat(match).isNotNull();
    assertThat(match.os()).isEqualTo(OperatingSystem.WINDOWS);
    assertThat(match.range()).isEqualTo(VersionRange.of("[1.0.0,2.0.0)"));
  }

  @Test
  void testToVersionRangeReturnsNullOsForGenericVulnerability() throws CpeValidationException {

    VulnerableSoftware software = software(b -> b.versionStartIncluding("1.0.0").versionEndExcluding("2.0.0"));

    OsVersionRange match = BuildSecurityJsonFiles.toVersionRange(software, TOOL, this.updater, "CVE-TEST");

    assertThat(match).isNotNull();
    assertThat(match.os()).isNull();
    assertThat(match.range()).isEqualTo(VersionRange.of("[1.0.0,2.0.0)"));
  }

  @Test
  void testToVersionRangeReturnsNullForNonMatchingCpe() throws CpeValidationException {

    VulnerableSoftware software = software("other", "other", b -> b.versionStartIncluding("1.0.0"));

    assertThat(BuildSecurityJsonFiles.toVersionRange(software, TOOL, this.updater, "CVE-TEST")).isNull();
  }

  @Test
  void testToVersionsSeparatesGeneralAndOsSpecificRanges() throws CpeValidationException {

    // arrange
    Vulnerability vulnerability = new Vulnerability("CVE-2024-12345");
    vulnerability.addVulnerableSoftware(software(b -> b.versionStartIncluding("1.0.0").versionEndExcluding("1.5.0")));
    vulnerability.addVulnerableSoftware(software(b -> b.targetSw("windows").versionStartIncluding("2.0.0").versionEndIncluding("2.0.8")));
    vulnerability.addVulnerableSoftware(software(b -> b.targetSw("linux").versionStartIncluding("2.0.0").versionEndIncluding("2.0.5")));

    // act
    Cve cve = BuildSecurityJsonFiles.toCve(vulnerability, TOOL, this.updater, 7.5);

    // assert
    assertThat(cve).isNotNull();
    assertThat(cve.id()).isEqualTo("CVE-2024-12345");
    assertThat(cve.severity()).isEqualTo(7.5);
    assertThat(cve.versions()).containsExactly(VersionRange.of("[1.0.0,1.5.0)"));
    assertThat(cve.conditions()).containsOnlyKeys("windows", "linux");
    assertThat(cve.conditions().get("windows")).containsExactly(VersionRange.of("[2.0.0,2.0.8]"));
    assertThat(cve.conditions().get("linux")).containsExactly(VersionRange.of("[2.0.0,2.0.5]"));
  }

  @Test
  void testToVersionsWithOnlyOsSpecificSoftwareLeavesGeneralVersionsEmpty() throws CpeValidationException {

    // arrange
    Vulnerability vulnerability = new Vulnerability("CVE-2024-99999");
    vulnerability.addVulnerableSoftware(software(b -> b.targetSw("windows").versionStartIncluding("1.0.0").versionEndExcluding("1.2.0")));

    // act
    Cve cve = BuildSecurityJsonFiles.toCve(vulnerability, TOOL, this.updater, 7.5);

    // assert
    assertThat(cve).isNotNull();
    assertThat(cve.versions()).isEmpty();
    assertThat(cve.conditions()).containsOnlyKeys("windows");
    assertThat(cve.conditions().get("windows")).containsExactly(VersionRange.of("[1.0.0,1.2.0)"));
  }

  @Test
  void testToCveReturnsNullWithoutMatchingSoftware() throws CpeValidationException {

    Vulnerability vulnerability = new Vulnerability("CVE-2024-00000");
    vulnerability.addVulnerableSoftware(software("other", "other", b -> b.versionStartIncluding("1.0.0")));

    assertThat(BuildSecurityJsonFiles.toCve(vulnerability, TOOL, this.updater, 7.5)).isNull();
  }

  private static VulnerableSoftware software(Consumer<VulnerableSoftwareBuilder> customizer) throws CpeValidationException {

    return software(TOOL, TOOL, customizer);
  }

  private static VulnerableSoftware software(String vendor, String product, Consumer<VulnerableSoftwareBuilder> customizer)
      throws CpeValidationException {

    VulnerableSoftwareBuilder builder = new VulnerableSoftwareBuilder();
    builder.part(Part.APPLICATION).vendor(vendor).product(product);
    customizer.accept(builder);
    return builder.build();
  }

  private static final class TestCpeUpdater extends AbstractUrlUpdater {

    TestCpeUpdater() {

      super("https://example.org", "https://example.org");
    }

    @Override
    public String getTool() {

      return TOOL;
    }

    @Override
    protected Set<String> getVersions() {

      return Set.of();
    }

    @Override
    protected void addVersion(UrlVersion urlVersion) {

      // not needed for these tests
    }
  }

}
