package com.devonfw.tools.ide.url.model;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.file.UrlSecurityJsonFile;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/** Test of {@link UrlSecurityJsonFile}. */

public class UrlSecurityJsonFileTest extends AbstractIdeContextTest {

  /** Test of {@link UrlSecurityJsonFile#load(boolean)}} */
  @Test
  public void testUrlJsonSecurityFileLoad() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    UrlSecurityWarning warning1 = new UrlSecurityWarning();
    warning1.setVersionRange(VersionRange.of("[3.0.6,3.2.1)"));
    warning1.setSeverity(BigDecimal.valueOf(5.8));
    warning1.setCveName("testName1");
    warning1.setDescription("testDescription1");
    warning1.setNistUrl("https://nvd.nist.gov/vuln/detail/testName1");
    UrlSecurityWarning warning2 = new UrlSecurityWarning();
    warning2.setVersionRange(VersionRange.of("(,3.8.1)"));
    warning2.setSeverity(BigDecimal.valueOf(9.1));
    warning2.setCveName("testName2");
    warning2.setDescription("testDescription2");
    warning2.setNistUrl("https://nvd.nist.gov/vuln/detail/testName2");

    // act
    IdeContext context = newContext("basic", path, true);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition("mvn", "mvn").getSecurityJsonFile();

    // assert
    assertThat(securityFile.getUrlSecurityWarnings()).containsExactly(warning1, warning2);
  }

  /**
   * Test of {@link UrlSecurityJsonFile#save()} and
   * {@link UrlSecurityJsonFile#addSecurityWarning(VersionRange, BigDecimal, String, String, String)}
   */
  @Test
  public void testUrlJsonSecurityFileAddAndSave() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition("mvn", "mvn").getSecurityJsonFile();
    Path securityFilePath = securityFile.getPath();

    // act
    securityFile.clearSecurityWarnings();
    securityFile.addSecurityWarning(VersionRange.of("[1,3)"), BigDecimal.valueOf(1.2), "testName3", "testDescription3",
        "https://nvd.nist.gov/vuln/detail/testName3");
    securityFile.save();

    // assert
    assertThat(new File(String.valueOf(securityFilePath))).hasContent("""
        [ {
          "versionRange" : "[1,3)",
          "severity" : 1.2,
          "cveName" : "testName3",
          "description" : "testDescription3",
          "nistUrl" : "https://nvd.nist.gov/vuln/detail/testName3"
        } ]
        """);
  }

  /** Test of {@link UrlSecurityJsonFile#contains(VersionIdentifier)} */
  @Test
  public void testUrlSecurityJsonFileContains() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition("mvn", "mvn").getSecurityJsonFile();

    // act & assert
    assertThat(securityFile.contains(VersionIdentifier.of("3.5"))).isTrue();
    assertThat(securityFile.contains(VersionIdentifier.of("3.8.1"))).isFalse();
  }

  /**
   * Test of {@link UrlSecurityJsonFile#contains(VersionIdentifier, boolean, IdeContext, UrlEdition)} where
   * {@code ignoreWarningsThatAffectAllVersions} is {@code true}.
   */
  @Test
  public void testUrlSecurityJsonFileContainsIgnoreWarningsThatAffectAllVersions() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    UrlEdition edition = context.getUrls().getEdition("mvn", "mvn");
    UrlSecurityJsonFile securityFile = edition.getSecurityJsonFile();

    // act & assert
    assertThat(securityFile.contains(VersionIdentifier.of("3.5"), true, context, edition)).isFalse();
    assertThat(securityFile.contains(VersionIdentifier.of("3.1"))).isTrue();
  }

  /** Test of {@link UrlSecurityJsonFile#getMatchingSecurityWarnings(VersionIdentifier)}. */
  @Test
  public void testGetMatchingSecurityWarnings() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    UrlEdition edition = context.getUrls().getEdition("mvn", "mvn");
    UrlSecurityJsonFile securityFile = edition.getSecurityJsonFile();
    UrlSecurityWarning warning1 = new UrlSecurityWarning();
    warning1.setVersionRange(VersionRange.of("[3.0.6,3.2.1)"));
    warning1.setSeverity(BigDecimal.valueOf(5.8));
    warning1.setCveName("testName1");
    warning1.setDescription("testDescription1");
    warning1.setNistUrl("https://nvd.nist.gov/vuln/detail/testName1");
    UrlSecurityWarning warning2 = new UrlSecurityWarning();
    warning2.setVersionRange(VersionRange.of("(,3.8.1)"));
    warning2.setSeverity(BigDecimal.valueOf(9.1));
    warning2.setCveName("testName2");
    warning2.setDescription("testDescription2");
    warning2.setNistUrl("https://nvd.nist.gov/vuln/detail/testName2");

    // act
    Set<UrlSecurityWarning> warnings1 = securityFile.getMatchingSecurityWarnings(VersionIdentifier.of("3.2"));
    Set<UrlSecurityWarning> warnings2 = securityFile.getMatchingSecurityWarnings(VersionIdentifier.of("1.2.3"));
    Set<UrlSecurityWarning> warnings3 = securityFile.getMatchingSecurityWarnings(VersionIdentifier.of("4"));

    // assert
    assertThat(warnings1).hasSize(2);
    assertThat(warnings1).containsExactly(warning1, warning2);
    assertThat(warnings2).hasSize(1);
    assertThat(warnings2).containsExactly(warning2);
    assertThat(warnings3).isEmpty();
  }
}
