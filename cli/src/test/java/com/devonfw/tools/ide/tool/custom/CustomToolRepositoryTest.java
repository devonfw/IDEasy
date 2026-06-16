package com.devonfw.tools.ide.tool.custom;

import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link CustomToolRepository}.
 */
@WireMockTest
class CustomToolRepositoryTest extends Assertions {

  private static final String ORIGINAL_BASE_URL = "https://some-file-server.company.com";
  private static final Path SETTINGS_PATH = Path.of("src/test/resources/customtools");

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
    assertThat(this.repository.getTools()).hasSize(2);
  }

  /**
   * Tests that unknown properties in custom-tools.json are silently ignored and do not break loading.
   *
   * @param tempDir temporary test directory used as working directory and settings base.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  @Test
  void testUnknownPropertiesInJsonAreIgnored(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    setUpRepository(tempDir, wmRuntimeInfo);

    // act & assert
    assertThatNoException().isThrownBy(() -> CustomToolRepositoryImpl.of(this.context));
    assertThat(this.repository.getTools()).hasSize(2);
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
    CustomToolMetadata tool = new CustomToolMetadata("extra", "1.0", null, null, "https://example.com/extra.tgz",
        null, "https://example.com");

    // act & assert
    assertThatThrownBy(() -> this.repository.getTools().add(tool)).isInstanceOf(UnsupportedOperationException.class);
  }

  /**
   * Creates a {@link CustomToolRepository} for the current test using the default test {@link SystemInfo}.
   * <p>
   * The repository configuration is copied from the test resources into the temporary test settings directory and all configured repository URLs are rewritten
   * to point to the active WireMock server.
   *
   * @param tempDir the temporary test directory used as isolated working directory and parent directory for the generated settings folder.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  private void setUpRepository(Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    setUpRepository(tempDir, wmRuntimeInfo, null);
  }

  /**
   * Creates a fully initialized {@link CustomToolRepository} for the current test.
   *
   * @param tempDir the temporary test directory used as isolated working directory and parent directory for the generated settings folder.
   * @param wmRuntimeInfo wireMock server on a random port.
   * @param systemInfo optional {@link SystemInfo} to use for the test, or {@code null} to keep the default.
   */
  private void setUpRepository(Path tempDir, WireMockRuntimeInfo wmRuntimeInfo, SystemInfo systemInfo) {
    this.context = new IdeTestContext(tempDir, wmRuntimeInfo);
    if (systemInfo != null) {
      this.context.setSystemInfo(systemInfo);
    }
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

}
