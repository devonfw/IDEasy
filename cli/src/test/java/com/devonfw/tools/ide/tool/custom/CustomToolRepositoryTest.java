package com.devonfw.tools.ide.tool.custom;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link CustomToolRepository}.
 */
@WireMockTest
class CustomToolRepositoryTest extends Assertions {

  private static final String ORIGINAL_BASE_URL = "https://some-file-server.company.com";
  private static final Path SETTINGS_PATH = Path.of("src/test/resources/customtools");
  private static final String BASE_PATH = "/projects/my-project";
  private static final String TOOL_JBOSS_EAP = "jboss-eap";
  private static final String VERSION_JBOSS = "7.1.4.GA";
  private static final String TOOL_FIREFOX = "firefox";
  private static final String VERSION_FIREFOX = "70.0.1";

  private IdeTestContext context;
  private CustomToolRepository repository;

  /**
   * Tests that the repository can be loaded from custom-tools.json and provides the correct number of tools.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testLoadRepositoryFromJson(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // assert
    assertThat(this.repository).isNotNull();
    assertThat(this.repository.getTools()
        .stream()
        .map(tool -> tool.getTool() + ":" + tool.getVersion()))
        .contains("jboss-eap:7.1.4.GA", "firefox:70.0.1");
  }

  /**
   * Tests that the custom tool collection exposed by the repository is immutable.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testGetToolsReturnsImmutableCollection(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    CustomToolMetadata tool = new CustomToolMetadata("extra", "1.0", null, null, "https://example.com/extra.tgz", null, "https://example.com");

    // act & assert
    assertThatThrownBy(() -> this.repository.getTools().add(tool)).isInstanceOf(UnsupportedOperationException.class);
  }

  /**
   * Tests that the repository correctly provides tool metadata for os-agnostic tools.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testGetToolMetadataOsAgnostic(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act
    CustomToolMetadata jbossEap = findTool(TOOL_JBOSS_EAP);

    // assert
    assertThat(jbossEap).isNotNull();
    assertThat(jbossEap.getTool()).isEqualTo(TOOL_JBOSS_EAP);
    assertThat(jbossEap.getVersion()).isEqualTo(VersionIdentifier.of(VERSION_JBOSS));
    assertThat(jbossEap.getOs()).isNull();
    assertThat(jbossEap.getArch()).isNull();
    assertThat(jbossEap.getUrl()).contains(BASE_PATH);
  }

  // --- Version resolution --------------------------------------------------

  /**
   * Tests that the repository correctly resolves version constraints.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testResolveVersionForTool(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    GenericVersionRange versionRange = VersionIdentifier.of(VERSION_JBOSS);

    // act
    VersionIdentifier resolved = this.repository.resolveVersion(TOOL_JBOSS_EAP, TOOL_JBOSS_EAP, versionRange, null);

    // assert
    assertThat(resolved).isEqualTo(VersionIdentifier.of(VERSION_JBOSS));
  }

  /**
   * Tests that a version that does not satisfy the constraint throws an exception.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testResolveInvalidVersionThrowsException(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    GenericVersionRange versionRange = VersionRange.of("8.0");

    // act & assert
    assertThatThrownBy(() -> this.repository.resolveVersion(TOOL_JBOSS_EAP, TOOL_JBOSS_EAP, versionRange, null)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not satisfy version");
  }

  // --- Downloads via WireMock ----------------------------------------------

  /**
   * Tests that a 404 response from the file server causes download to throw.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testDownloadReturns404ThrowsException(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    String expectedPath = jbossEapPath();

    stubFor(get(urlEqualTo(expectedPath)).willReturn(aResponse().withStatus(404)));

    // act & assert
    assertThatThrownBy(() -> this.repository.download(TOOL_JBOSS_EAP, TOOL_JBOSS_EAP, VersionIdentifier.of(VERSION_JBOSS), null))
        .isInstanceOf(IllegalStateException.class);
  }

  /**
   * Tests that a 500 server error causes download to throw.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testDownloadReturns500ThrowsException(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    String expectedPath = jbossEapPath();

    stubFor(get(urlEqualTo(expectedPath)).willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    // act & assert
    assertThatThrownBy(() -> this.repository.download(TOOL_JBOSS_EAP, TOOL_JBOSS_EAP, VersionIdentifier.of(VERSION_JBOSS), null))
        .isInstanceOf(IllegalStateException.class);
  }

  /**
   * Tests that download validates the requested version against the custom tool metadata.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testDownloadWithUndefinedVersionThrowsException(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act & assert
    assertThatThrownBy(() -> this.repository.download(TOOL_JBOSS_EAP, TOOL_JBOSS_EAP, VersionIdentifier.of("7.1.5.GA"), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Undefined version")
        .hasMessageContaining(VERSION_JBOSS);
  }

  // --- Repository metadata ----------------------------------------------------

  /**
   * Tests that {@link com.devonfw.tools.ide.url.model.AbstractUrlMetadata#getSortedVersions(String, String, ToolCommandlet)} returns the single configured
   * custom tool version.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testGetSortedVersions(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act
    List<VersionIdentifier> versions = this.repository.getSortedVersions(TOOL_FIREFOX, TOOL_FIREFOX, null);

    // assert
    assertThat(versions).containsExactly(VersionIdentifier.of(VERSION_FIREFOX));
  }

  /**
   * Tests that {@link com.devonfw.tools.ide.url.model.AbstractUrlMetadata#getSortedEditions(String)} returns the configured custom tool edition.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testGetSortedEditions(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act & assert
    assertThat(this.repository.getSortedEditions(TOOL_FIREFOX)).containsExactly(TOOL_FIREFOX);
  }

  /**
   * Tests that custom tools do not define additional dependencies.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFindDependenciesReturnsEmptyCollection(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act & assert
    assertThat(this.repository.findDependencies(TOOL_FIREFOX, TOOL_FIREFOX, VersionIdentifier.of(VERSION_FIREFOX))).isEmpty();
  }

  /**
   * Tests that custom tools return empty security metadata.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testFindSecurityReturnsEmptySecurity(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act
    ToolSecurity security = this.repository.findSecurity(TOOL_FIREFOX, TOOL_FIREFOX);

    // assert
    assertThat(security).isSameAs(ToolSecurity.getEmpty());
  }

  // --- Commandlet -------------------------------------------------------------

  /**
   * Tests that {@link CustomToolCommandlet} exposes the configured custom tool values.
   */
  @Test
  void testCreateCustomToolCommandlet(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = new IdeTestContext(tempDir, wmRuntimeInfo);
    String repositoryUrl = "https://example.com/repo";
    CustomToolMetadata customTool = new CustomToolMetadata("custom-cli", "1.2.3", null, null,
        repositoryUrl + "/custom-cli/1.2.3/custom-cli-1.2.3.tgz", null, repositoryUrl);

    // act
    CustomToolCommandlet commandlet = new CustomToolCommandlet(context, customTool);

    // assert
    assertThat(commandlet.getName()).isEqualTo("custom-cli");
    assertThat(commandlet.getConfiguredVersion()).isEqualTo(VersionIdentifier.of("1.2.3"));
    assertThat(commandlet.getConfiguredEdition()).isEqualTo("custom-cli");
    assertThat(commandlet.getTags()).isNull();
  }

  /**
   * Tests that {@link CustomToolCommandlet} delegates repository access to the context.
   *
   * @param tempDir the temporary test directory used as isolated working directory and parent directory for the generated settings folder.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testCustomToolCommandletReturnsContextCustomToolRepository(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);
    CustomToolMetadata customTool = findTool(TOOL_JBOSS_EAP);
    CustomToolCommandlet commandlet = new CustomToolCommandlet(this.context, customTool);

    // act
    ToolRepository toolRepository = commandlet.getToolRepository();

    // assert
    assertThat(toolRepository).isSameAs(this.context.getCustomToolRepository());
  }

  /**
   * Creates a fully initialized {@link CustomToolRepository} for the current test.
   *
   * @param tempDir the temporary test directory used as isolated working directory and parent directory for the generated settings folder.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  private void setUpRepository(Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    this.context = new IdeTestContext(tempDir, wmRuntimeInfo);
    this.context.getNetworkStatus().simulateOnline();

    Path settingsPath = tempDir.resolve("settings");
    this.context.getFileAccess().mkdirs(settingsPath);

    CustomTools customTools = CustomToolsMapper.get().loadJsonFromFolder(SETTINGS_PATH);
    CustomTools resolvedCustomTools = resolveWireMockUrl(customTools, wmRuntimeInfo.getHttpBaseUrl());

    CustomToolsMapper.get().saveJson(resolvedCustomTools, settingsPath.resolve(CustomToolsMapper.get().getStandardFilename()));

    this.context.setSettingsPath(settingsPath);
    this.repository = CustomToolRepositoryImpl.of(this.context);
  }

  /**
   * Replaces the static repository base URL from the test resource with the runtime base URL of the WireMock server.
   * <p>
   * The custom tools JSON contains stable, readable URls that model a real file server. During tests, downloads must be served by WireMock instead. This method
   * keeps the configured paths unchanged while replacing only the host/base URL, so assertions can still verify the expected repository paths.
   *
   * @param customTools the custom tools configuration loaded from the test resource.
   * @param wireMockBaseUrl the base URL of the active WireMock server.
   * @return a copy of {@code customTools} with all repository URLs pointing to WireMock.
   */
  private CustomTools resolveWireMockUrl(CustomTools customTools, String wireMockBaseUrl) {
    List<CustomTool> tools = customTools.tools().stream().map(tool -> {
      String url = tool.url();
      if (url != null) {
        url = url.replace(ORIGINAL_BASE_URL, wireMockBaseUrl);
      }
      return new CustomTool(tool.name(), tool.version(), tool.osAgnostic(), tool.archAgnostic(), url);
    }).toList();

    String url = customTools.url().replace(ORIGINAL_BASE_URL, wireMockBaseUrl);
    return new CustomTools(url, tools);
  }

  /**
   * Finds the configured custom tool metadata by tool name.
   *
   * @param toolName the name of the custom tool to find.
   * @return the matching {@link CustomToolMetadata}, or {@code null} if no such tool is configured.
   */
  private CustomToolMetadata findTool(String toolName) {
    return this.repository.getTools().stream().filter(t -> t.getTool().equals(toolName)).findFirst().orElse(null);
  }

  private static String jbossEapPath() {
    return BASE_PATH + "/" + TOOL_JBOSS_EAP + "/" + VERSION_JBOSS + "/" + TOOL_JBOSS_EAP + "-" + VERSION_JBOSS + ".tgz";
  }
}
