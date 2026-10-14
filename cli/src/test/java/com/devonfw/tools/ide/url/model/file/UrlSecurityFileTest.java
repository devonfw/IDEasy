package com.devonfw.tools.ide.url.model.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.url.model.AbstractUrlModelTest;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link UrlSecurityFile}.
 */
class UrlSecurityFileTest extends AbstractUrlModelTest {

  /**
   * @return a new, not yet loaded {@link UrlEdition} backed by the urls test-fixture that does not exist on disk yet.
   */
  private UrlEdition newEdition() {

    return newRepo().getOrCreateChild("test-tool").getOrCreateChild("test-edition");
  }

  /**
   * @param tempDir the {@link Path} to use as repository.
   * @return a new {@link UrlEdition} backed by the given {@code tempDir} whose folder already exists on disk.
   * @throws IOException if the directory of the {@link UrlEdition} could not be created.
   */
  private UrlEdition newTempEdition(Path tempDir) throws IOException {

    UrlEdition edition = new UrlRepository(tempDir).getOrCreateChild("test-tool").getOrCreateChild("test-edition");
    Files.createDirectories(edition.getPath());
    return edition;
  }

  /**
   * @param range the single {@link VersionRange} of the CVE to add.
   * @return a new, empty {@link UrlSecurityFile} with one CVE for the given {@code range}.
   */
  private UrlSecurityFile newFileWithRange(String range) {

    UrlSecurityFile file = newEdition().getSecurityFile();
    file.addCve(new Cve("CVE-TEST", 5.0, List.of(VersionRange.of(range))));
    return file;
  }

  /**
   * Verifies that {@link UrlSecurityFile#getSecurity()} returns the empty {@link ToolSecurity} before any data was loaded.
   */
  @Test
  void testGetSecurityEmptyBeforeLoad() {

    // arrange
    UrlSecurityFile file = newEdition().getSecurityFile();

    // assert
    assertThat(file.getSecurity()).isSameAs(ToolSecurity.getEmpty());
  }

  /**
   * Verifies that an existing {@code security.json} file is loaded correctly.
   */
  @Test
  void testLoadExistingSecurityFile() {

    // arrange
    UrlTool intellij = newRepo().getChild("intellij");

    // act
    ToolSecurity security = intellij.getSecurityFile().getSecurity();

    // assert
    assertThat(security.getIssues()).contains(new Cve("CVE-2024-32002", 9.0,
        List.of(VersionRange.of("(0,2.39.4)"), VersionRange.of("[2.40.0,2.40.2)"))));
  }

  /**
   * Verifies that {@link UrlSecurityFile#setSecurity(ToolSecurity)} replaces the current security data and marks the file as modified.
   */
  @Test
  void testSetSecurityReplacesData() {

    // arrange
    UrlSecurityFile file = newEdition().getSecurityFile();
    ToolSecurity initial = new ToolSecurity(List.of(new Cve("CVE-INITIAL", 5.0, List.of(VersionRange.of("[1.0,2.0]")))));
    ToolSecurity replacement = new ToolSecurity(List.of(new Cve("CVE-REPLACEMENT", 4.0, List.of(VersionRange.of("[3.0,4.0]")))));

    // act
    file.setSecurity(initial);
    assertThat(file.getSecurity()).isSameAs(initial);
    file.setSecurity(replacement);

    // assert
    assertThat(file.getSecurity()).isSameAs(replacement);
    assertThat(file.getSecurity().getIssues()).hasSize(1);
    assertThat(file.modified).isTrue();
  }

  /**
   * Verifies that {@link UrlSecurityFile#addCve(Cve)} adds a new CVE and does not create duplicates for an already contained CVE.
   */
  @Test
  void testAddCve() {

    // arrange
    UrlSecurityFile file = newEdition().getSecurityFile();
    Cve cve = new Cve("CVE-NEW", 5.0, List.of(VersionRange.of("[1.0,2.0]")));

    // act
    file.addCve(cve);
    file.addCve(new Cve("CVE-NEW", 5.0, List.of(VersionRange.of("[1.0,2.0]"))));

    // assert
    assertThat(file.getSecurity().getIssues()).containsExactly(cve);
  }

  /**
   * Verifies that {@link UrlSecurityFile#addCve(Cve)} marks the file as modified for a new CVE, but not for an already contained CVE.
   */
  @Test
  void testAddCveSetsModified() {

    // arrange
    UrlSecurityFile file = newRepo().getChild("intellij").getSecurityFile(); // loaded from fixture, not modified
    Cve existing = new Cve("Test1", 1.0, List.of(VersionRange.of("[2022.3]")));

    // act
    file.addCve(existing);

    // assert: duplicate CVE does not mark the file as modified
    assertThat(file.modified).isFalse();
    assertThat(file.getSecurity().getIssues()).hasSize(7);

    // act
    file.addCve(new Cve("CVE-NEW", 5.0, List.of(VersionRange.of("[9.9.9]"))));

    // assert: new CVE marks the file as modified
    assertThat(file.modified).isTrue();
  }

  /**
   * Verifies that {@link UrlSecurityFile#clearSecurityWarnings()} removes all CVEs and marks the file as modified. It must be a no-op when no security was
   * loaded.
   */
  @Test
  void testClearSecurityWarnings() {

    // arrange
    UrlSecurityFile file = newRepo().getChild("intellij").getSecurityFile();

    // act
    file.clearSecurityWarnings();

    // assert
    assertThat(file.getSecurity().getIssues()).isEmpty();
    assertThat(file.modified).isTrue();

    // arrange: file that was never loaded has no security
    UrlSecurityFile fresh = new UrlSecurityFile(newEdition());

    // act + assert: must be a no-op
    fresh.clearSecurityWarnings();
    assertThat(fresh.getSecurity()).isSameAs(ToolSecurity.getEmpty());
  }

  /**
   * Verifies that {@link UrlSecurityFile#contains(VersionIdentifier)} respects the version ranges of the CVEs including inclusive and exclusive boundaries and
   * returns {@code false} when no security is present.
   */
  @Test
  void testContainsVersionInCveRange() {

    // arrange
    UrlSecurityFile bounded = newFileWithRange("[2.0,3.0)");
    UrlSecurityFile openStart = newFileWithRange("(,3.0]");
    UrlSecurityFile openEnd = newFileWithRange("[1.0,)");
    UrlSecurityFile empty = newEdition().getSecurityFile();

    // assert
    assertThat(bounded.contains(VersionIdentifier.of("2.0"))).isTrue(); // inclusive lower bound
    assertThat(bounded.contains(VersionIdentifier.of("2.5"))).isTrue(); // within range
    assertThat(bounded.contains(VersionIdentifier.of("3.0"))).isFalse(); // exclusive upper bound
    assertThat(bounded.contains(VersionIdentifier.of("1.0"))).isFalse(); // below range
    assertThat(openStart.contains(VersionIdentifier.of("3.0"))).isTrue(); // inclusive upper bound
    assertThat(openStart.contains(VersionIdentifier.of("4.0"))).isFalse(); // above range
    assertThat(openEnd.contains(VersionIdentifier.of("1.0"))).isTrue(); // inclusive lower bound
    assertThat(openEnd.contains(VersionIdentifier.of("99.0"))).isTrue(); // above (unbounded) range
    assertThat(empty.contains(VersionIdentifier.of("2.5"))).isFalse(); // no security loaded
  }

  /**
   * Verifies that {@link UrlSecurityFile#contains(VersionIdentifier, boolean, IdeContext, UrlEdition)} ignores warnings affecting all available versions when
   * {@code ignoreWarningsThatAffectAllVersions} is {@code true}, while warnings covering only part of the versions are still detected.
   */
  @Test
  void testContainsIgnoresAllVersionWarnings() {

    // arrange
    IdeContext context = newContext();
    ((AbstractIdeTestContext) context).setSystemInfo(new SystemInfoImpl("Linux", "12", SystemArchitecture.X64.toString()));
    UrlEdition edition = context.getUrls().getEdition("intellij", "intellij");
    UrlSecurityFile file = edition.getParent().getSecurityFile();
    file.clearSecurityWarnings();
    VersionIdentifier version = VersionIdentifier.of("2023.3.3");

    // act: CVE spanning the oldest (2022.3) to the newest (2025.1.1.1) available version is ignored
    file.addCve(new Cve("CVE-ALL", 5.0, List.of(VersionRange.of("[2022.3,2025.1.1.1]"))));
    boolean spanningAllVersions = file.contains(version, true, context, edition);

    // act: CVE only covering part of the available versions is still detected
    file.clearSecurityWarnings();
    file.addCve(new Cve("CVE-PARTIAL", 5.0, List.of(VersionRange.of("[2022.3,2023.3.3]"))));
    boolean partialVersions = file.contains(version, true, context, edition);

    // assert
    assertThat(spanningAllVersions).isFalse();
    assertThat(partialVersions).isTrue();
  }

  /**
   * Verifies that {@link UrlSecurityFile#save()} does not create the file when there are no warnings and the file does not exist yet.
   */
  @Test
  void testSaveSkipsWhenNoWarningsAndFileDoesNotExist(@TempDir Path tempDir) throws IOException {

    // arrange
    UrlSecurityFile file = newTempEdition(tempDir).getSecurityFile();
    // mark as modified so that doSave() is actually executed and its skip branch is tested
    file.setSecurity(ToolSecurity.getEmpty());

    // act
    file.save();

    // assert
    assertThat(file.getPath()).doesNotExist();
  }

  /**
   * Verifies that {@link UrlSecurityFile#save()} writes the security data to disk and that the written file can be loaded again.
   */
  @Test
  void testSaveRoundTrip(@TempDir Path tempDir) throws IOException {

    // arrange
    UrlEdition edition = newTempEdition(tempDir);
    UrlSecurityFile file = edition.getSecurityFile();
    Cve cve = new Cve("CVE-NEW", 5.0, List.of(VersionRange.of("[1.0,2.0]")));
    file.addCve(cve);

    // act
    file.save();

    // assert
    assertThat(file.getPath()).exists();
    UrlSecurityFile reloaded = new UrlSecurityFile(edition);
    reloaded.load(false);
    assertThat(reloaded.getSecurity().getIssues()).containsExactly(cve);
  }
}
