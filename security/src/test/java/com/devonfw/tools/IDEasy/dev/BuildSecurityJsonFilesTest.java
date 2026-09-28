package com.devonfw.tools.IDEasy.dev;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.dependency.VulnerableSoftwareBuilder;

import com.devonfw.tools.IDEasy.dev.BuildSecurityJsonFiles.OsVersionRange;
import com.devonfw.tools.ide.os.OperatingSystem;
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
  void testToVersionRangeRoutesOsSpecificRangeToOs() throws CpeValidationException {

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
    Map<String, List<VersionRange>> conditions = new TreeMap<>();

    // act
    List<VersionRange> versions = BuildSecurityJsonFiles.toVersions(vulnerability, TOOL, this.updater, conditions);

    // assert
    assertThat(versions).containsExactly(VersionRange.of("[1.0.0,1.5.0)"));
    assertThat(conditions).containsOnlyKeys("windows", "linux");
    assertThat(conditions.get("windows")).containsExactly(VersionRange.of("[2.0.0,2.0.8]"));
    assertThat(conditions.get("linux")).containsExactly(VersionRange.of("[2.0.0,2.0.5]"));
  }

  @Test
  void testToVersionsWithOnlyOsSpecificSoftwareLeavesGeneralVersionsEmpty() throws CpeValidationException {

    // arrange
    Vulnerability vulnerability = new Vulnerability("CVE-2024-99999");
    vulnerability.addVulnerableSoftware(software(b -> b.targetSw("windows").versionStartIncluding("1.0.0").versionEndExcluding("1.2.0")));
    Map<String, List<VersionRange>> conditions = new TreeMap<>();

    // act
    List<VersionRange> versions = BuildSecurityJsonFiles.toVersions(vulnerability, TOOL, this.updater, conditions);

    // assert
    assertThat(versions).isEmpty();
    assertThat(conditions).containsOnlyKeys("windows");
    assertThat(conditions.get("windows")).containsExactly(VersionRange.of("[1.0.0,1.2.0)"));
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
