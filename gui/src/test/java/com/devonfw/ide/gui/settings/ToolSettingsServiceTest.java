package com.devonfw.ide.gui.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link ToolSettingsService}.
 */
class ToolSettingsServiceTest extends AbstractIdeContextTest {

  private final ToolSettingsService service = new ToolSettingsService();

  // ---------------------------------------------------------------------------
  // toToolConfiguration
  // ---------------------------------------------------------------------------

  @Test
  void testToToolConfiguration_toolNameMatchesCommandletName() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    ToolConfiguration tc = service.toToolConfiguration(new Mvn(context), List.of());

    assertThat(tc.getToolName()).isEqualTo("mvn");
  }

  @Test
  void testToToolConfiguration_enabledWhenInEnabledToolsList() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    ToolConfiguration tc = service.toToolConfiguration(new Mvn(context), List.of("mvn", "npm"));

    assertThat(tc.isEnabled()).isTrue();
  }

  @Test
  void testToToolConfiguration_disabledWhenNotInEnabledToolsList() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    ToolConfiguration tc = service.toToolConfiguration(new Mvn(context), List.of("npm"));

    assertThat(tc.isEnabled()).isFalse();
  }

  @Test
  void testToToolConfiguration_enabledCheckIsCaseInsensitive() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    ToolConfiguration tc = service.toToolConfiguration(new Mvn(context), List.of("MVN"));

    assertThat(tc.isEnabled()).isTrue();
  }

  @Test
  void testToToolConfiguration_nullEnabledToolsListYieldsDisabled() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    ToolConfiguration tc = service.toToolConfiguration(new Mvn(context), null);

    assertThat(tc.isEnabled()).isFalse();
  }

  // ---------------------------------------------------------------------------
  // loadEditionsForTool
  // ---------------------------------------------------------------------------

  @Test
  void testLoadEditionsForTool_returnsEditionsFromRepository() {

    IdeTestContext context = newContext("testProject", "project-0", false);
    context.setDefaultToolRepository(stubRepo(List.of("community", "ultimate"), List.of()));

    List<String> editions = service.loadEditionsForTool("intellij", context);

    assertThat(editions).containsExactly("community", "ultimate");
  }

  @Test
  void testLoadEditionsForTool_returnsEmptyForUnknownTool() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    List<String> editions = service.loadEditionsForTool("nonexistent-xyz-123", context);

    assertThat(editions).isEmpty();
  }

  @Test
  void testLoadEditionsForTool_repositoryExceptionYieldsEmpty() {

    IdeTestContext context = newContext("testProject", "project-0", false);
    context.setDefaultToolRepository(stubRepo(null, null));

    List<String> editions = service.loadEditionsForTool("mvn", context);

    assertThat(editions).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // buildPreviewSettingsContent – pure logic, no context needed
  // ---------------------------------------------------------------------------

  @Test
  void testBuildPreview_singleEnabledToolWithVersion() {

    // arrange
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn));
    // assert
    assertThat(content).contains("IDE_TOOLS=mvn");
    assertThat(content).contains("MVN_VERSION=3.9.0");
  }

  @Test
  void testBuildPreview_disabledToolIsExcluded() {

    // arrange
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    ToolConfiguration npm = tool("npm", "10.0.0", null, false);
    npm.setEnabled(false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn, npm));
    // assert
    assertThat(content).contains("IDE_TOOLS=mvn");
    assertThat(content).doesNotContain("npm");
    assertThat(content).doesNotContain("NPM_VERSION");
  }

  @Test
  void testBuildPreview_nullVersionLineOmitted() {

    // arrange
    ToolConfiguration mvn = enabled("mvn", null, null, false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn));
    // assert
    assertThat(content).contains("IDE_TOOLS=mvn");
    assertThat(content).doesNotContain("MVN_VERSION");
  }

  @Test
  void testBuildPreview_blankVersionLineOmitted() {

    // arrange
    ToolConfiguration mvn = enabled("mvn", "  ", null, false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn));
    // assert
    assertThat(content).contains("IDE_TOOLS=mvn");
    assertThat(content).doesNotContain("MVN_VERSION");
  }

  @Test
  void testBuildPreview_editionNotWrittenWhenSupportsEditionIsFalse() {

    // arrange – edition is set but supportsEdition=false, so the guard must suppress it
    ToolConfiguration mvn = enabled("mvn", "3.9.0", "default", false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn));
    // assert
    assertThat(content).contains("MVN_VERSION=3.9.0");
    assertThat(content).doesNotContain("MVN_EDITION");
  }


  @Test
  void testBuildPreview_noEnabledTools() {

    // arrange
    ToolConfiguration mvn = tool("mvn", "3.9.0", null, false);
    mvn.setEnabled(false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn));
    // assert
    assertThat(content).startsWith("IDE_TOOLS=");
    assertThat(content).doesNotContain("MVN_VERSION");
  }

  @Test
  void testBuildPreview_toolWithEditionAndVersion() {

    // arrange
    ToolConfiguration intellij = enabled("intellij", "2024.1", "community", true);
    // act
    String content = service.buildPreviewSettingsContent(List.of(intellij));
    // assert
    assertThat(content).contains("IDE_TOOLS=intellij");
    assertThat(content).contains("INTELLIJ_VERSION=2024.1");
    assertThat(content).contains("INTELLIJ_EDITION=community");
  }

  @Test
  void testBuildPreview_blankEditionOmitted() {

    // arrange
    ToolConfiguration intellij = enabled("intellij", "2024.1", "", true);
    // act
    String content = service.buildPreviewSettingsContent(List.of(intellij));
    // assert
    assertThat(content).contains("INTELLIJ_VERSION=2024.1");
    assertThat(content).doesNotContain("INTELLIJ_EDITION");
  }

  @Test
  void testBuildPreview_nullEditionOmitted() {

    // arrange
    ToolConfiguration intellij = enabled("intellij", "2024.1", null, true);
    // act
    String content = service.buildPreviewSettingsContent(List.of(intellij));
    // assert
    assertThat(content).contains("INTELLIJ_VERSION=2024.1");
    assertThat(content).doesNotContain("INTELLIJ_EDITION");
  }

  @Test
  void testBuildPreview_multipleEnabledToolsAllListed() {

    // arrange
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    ToolConfiguration npm = enabled("npm", "10.0.0", null, false);
    // act
    String content = service.buildPreviewSettingsContent(List.of(mvn, npm));
    // assert
    assertThat(content).contains("mvn").contains("npm");
    assertThat(content).contains("MVN_VERSION=3.9.0");
    assertThat(content).contains("NPM_VERSION=10.0.0");
  }

  // ---------------------------------------------------------------------------
  // applyAndSave – writes to a real EnvironmentVariablesPropertiesFile
  // ---------------------------------------------------------------------------

  @Test
  void testApplyAndSave_writesEnabledToolVersionToFile(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeGuiContext guiContext = createGuiContext(tempDir, "");
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).contains("IDE_TOOLS=mvn");
    assertThat(saved).contains("MVN_VERSION=3.9.0");
  }

  @Test
  void testApplyAndSave_disabledToolVersionNotWritten(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeGuiContext guiContext = createGuiContext(tempDir, "");
    ToolConfiguration npm = tool("npm", "10.0.0", null, false);
    npm.setEnabled(false);
    // act
    service.applyAndSave(List.of(npm), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).doesNotContain("NPM_VERSION=10.0.0");
  }

  @Test
  void testApplyAndSave_disabledToolRemovesExistingVersionLine(@TempDir Path tempDir) throws IOException {

    // arrange – existing file already has a version entry for mvn
    IdeGuiContext guiContext = createGuiContext(tempDir, "IDE_TOOLS=mvn\nMVN_VERSION=3.8.0\n");
    ToolConfiguration mvn = tool("mvn", "3.8.0", null, false);
    mvn.setEnabled(false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert – the version line must be removed when the tool is disabled
    String saved = readSettingsFile(tempDir);
    assertThat(saved).doesNotContain("MVN_VERSION=");
  }

  @Test
  void testApplyAndSave_createsBackupOfExistingFile(@TempDir Path tempDir) throws IOException {

    // arrange – file must exist for createBackupIfPossible to act
    IdeGuiContext guiContext = createGuiContext(tempDir, "IDE_TOOLS=mvn\n");
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert
    assertThat(tempDir.resolve("project/settings/ide.properties.bak")).exists();
  }

  @Test
  void testApplyAndSave_noBackupWhenFileAbsent(@TempDir Path tempDir) throws IOException {

    // arrange – no ide.properties → backup must not be created
    IdeGuiContext guiContext = createGuiContext(tempDir, null);
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert
    assertThat(tempDir.resolve("project/settings/ide.properties.bak")).doesNotExist();
  }

  @Test
  void testApplyAndSave_updatesIdeToolsListToReflectEnabledSet(@TempDir Path tempDir) throws IOException {

    // arrange – mvn+npm initially enabled, only mvn stays enabled
    IdeGuiContext guiContext = createGuiContext(tempDir, "IDE_TOOLS=mvn, npm\n");
    ToolConfiguration mvn = enabled("mvn", "3.9.0", null, false);
    ToolConfiguration npm = tool("npm", "10.0.0", null, false);
    npm.setEnabled(false);
    // act
    service.applyAndSave(List.of(mvn, npm), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).contains("IDE_TOOLS=mvn");
    assertThat(saved).doesNotContain("npm");
  }

  @Test
  void testApplyAndSave_withEditionWritesEditionAndVersionVariables(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeGuiContext guiContext = createGuiContext(tempDir, "");
    ToolConfiguration intellij = enabled("intellij", "2024.1", "community", true);
    // act
    service.applyAndSave(List.of(intellij), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).contains("INTELLIJ_VERSION=2024.1");
    assertThat(saved).contains("INTELLIJ_EDITION=community");
  }

  @Test
  void testApplyAndSave_blankVersionClearsExistingVersionLine(@TempDir Path tempDir) throws IOException {

    // arrange – pre-existing version; blank input should clear it via emptyToNull
    IdeGuiContext guiContext = createGuiContext(tempDir, "IDE_TOOLS=mvn\nMVN_VERSION=3.8.0\n");
    ToolConfiguration mvn = enabled("mvn", "   ", null, false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).doesNotContain("MVN_VERSION=");
  }

  @Test
  void testApplyAndSave_editionNotWrittenWhenSupportsEditionIsFalse(@TempDir Path tempDir) throws IOException {

    // arrange – edition is set but supportsEdition=false; the guard must suppress the variable
    IdeGuiContext guiContext = createGuiContext(tempDir, "");
    ToolConfiguration mvn = enabled("mvn", "3.9.0", "default", false);
    // act
    service.applyAndSave(List.of(mvn), guiContext);
    // assert
    String saved = readSettingsFile(tempDir);
    assertThat(saved).contains("MVN_VERSION=3.9.0");
    assertThat(saved).doesNotContain("MVN_EDITION");
  }

  // ---------------------------------------------------------------------------
  // reloadVersionsForSelectedEdition
  // ---------------------------------------------------------------------------

  @Test
  void testLoadVersionsForSelectedEdition_returnsVersionsFromRepository() {

    IdeTestContext context = newContext("testProject", "project-0", false);
    List<VersionIdentifier> stubVersions = List.of(VersionIdentifier.of("3.9.0"), VersionIdentifier.of("3.8.0"));
    context.setDefaultToolRepository(stubRepo(List.of(), stubVersions));

    List<String> versions = service.loadVersionsForSelectedEdition("mvn", "default", context);

    assertThat(versions).containsExactly("3.9.0", "3.8.0");
  }

  @Test
  void testLoadVersionsForSelectedEdition_nullEditionReturnsEmpty() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    List<String> versions = service.loadVersionsForSelectedEdition("mvn", null, context);

    assertThat(versions).isEmpty();
  }

  @Test
  void testLoadVersionsForSelectedEdition_blankEditionReturnsEmpty() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    List<String> versions = service.loadVersionsForSelectedEdition("mvn", "  ", context);

    assertThat(versions).isEmpty();
  }

  @Test
  void testLoadVersionsForSelectedEdition_unknownToolReturnsEmpty() {

    IdeTestContext context = newContext("testProject", "project-0", false);

    List<String> versions = service.loadVersionsForSelectedEdition("nonexistent-xyz-123", "community", context);

    assertThat(versions).isEmpty();
  }

  @Test
  void testLoadVersionsForSelectedEdition_repositoryExceptionYieldsEmpty() {

    IdeTestContext context = newContext("testProject", "project-0", false);
    context.setDefaultToolRepository(stubRepo(null, null));

    List<String> versions = service.loadVersionsForSelectedEdition("mvn", "default", context);

    assertThat(versions).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  /**
   * Creates a minimal IDEasy project structure and returns an {@link IdeGuiContext} pointing to it. The layout is:
   * {@code tempDir/project/settings/ide.properties} and {@code tempDir/project/workspaces/main/}, which satisfies
   *
   */
  private IdeGuiContext createGuiContext(Path tempDir, String idePropertiesContent) throws IOException {

    Path ideHome = tempDir.resolve("project");
    Files.createDirectories(ideHome.resolve("settings"));
    Files.createDirectories(ideHome.resolve("workspaces").resolve("main"));
    if (idePropertiesContent != null) {
      Files.writeString(ideHome.resolve("settings").resolve("ide.properties"), idePropertiesContent);
    }
    IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.WARNING, new IdeLogListenerBuffer());
    return new IdeGuiContext(startContext, ideHome.resolve("workspaces").resolve("main"));
  }

  private String readSettingsFile(Path tempDir) throws IOException {

    return Files.readString(tempDir.resolve("project/settings/ide.properties"));
  }

  /** Builds an enabled {@link ToolConfiguration}. */
  private ToolConfiguration enabled(String name, String version, String edition, boolean supportsEdition) {

    ToolConfiguration tc = tool(name, version, edition, supportsEdition);
    tc.setEnabled(true);
    return tc;
  }

  /** Builds a disabled {@link ToolConfiguration}. */
  private ToolConfiguration tool(String name, String version, String edition, boolean supportsEdition) {

    ToolConfiguration tc = new ToolConfiguration(name);
    tc.setConfiguredVersion(version);
    tc.setConfiguredEdition(edition);
    tc.setSupportsEdition(supportsEdition);
    return tc;
  }

  /**
   * @param editions list returned by {@link ToolRepository#getSortedEditions}, or {@code null} to throw instead.
   * @param versions list returned by {@link ToolRepository#getSortedVersions}, or {@code null} to throw instead.
   * @return a lightweight {@link ToolRepository} stub backed by the given lists.
   */
  private static StubToolRepository stubRepo(List<String> editions, List<VersionIdentifier> versions) {

    return new StubToolRepository(editions, versions);
  }

  /**
   * Minimal {@link ToolRepository} stub. Passing {@code null} for {@code editions} or {@code versions} causes the corresponding method to throw, which
   * exercises the exception-handling paths in {@link ToolSettingsService#toToolConfiguration}.
   */
  private record StubToolRepository(List<String> editions, List<VersionIdentifier> versions) implements ToolRepository {

    @Override
    public String getId() {

      return "stub";
    }

    @Override
    public List<String> getSortedEditions(String tool) {

      if (this.editions == null) {
        throw new RuntimeException("simulated repository failure");
      }
      return this.editions;
    }

    @Override
    public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet commandlet) {

      if (this.versions == null) {
        throw new RuntimeException("simulated repository failure");
      }
      return this.versions;
    }

    @Override
    public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version,
        ToolCommandlet commandlet) {

      return null;
    }

    @Override
    public Path download(String tool, String edition, VersionIdentifier version, ToolCommandlet commandlet) {

      return null;
    }

    @Override
    public Collection<ToolDependency> findDependencies(String tool, String edition, VersionIdentifier version) {

      return List.of();
    }

    @Override
    public ToolSecurity findSecurity(String tool, String edition) {

      return null;
    }
  }
}
