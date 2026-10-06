package com.devonfw.tools.ide.url.tool.flutter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link FlutterUrlUpdater}.
 */
@WireMockTest
class FlutterUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testFlutterUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    Path testDir = PATH_INTEGRATION_TEST.resolve("FlutterUrlUpdater");
    stubFor(get(urlMatching("/releases_linux\\.json")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(testDir.resolve("releases_linux.json"), wmRuntimeInfo))));
    stubFor(get(urlMatching("/releases_macos\\.json")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(testDir.resolve("releases_macos.json"), wmRuntimeInfo))));
    stubFor(get(urlMatching("/releases_windows\\.json")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(testDir.resolve("releases_windows.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/stable/(linux|macos|windows)/flutter_(linux|macos(_arm64)?|windows)_3\\.47\\.5-stable\\.(tar\\.xz|zip)")).willReturn(
        aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    FlutterUrlUpdater updater = new FlutterUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());
    // when
    update(updater, urlRepository);

    // then
    Path flutterEditionPath = tempDir.resolve("flutter").resolve("flutter");
    assertUrlVersionOsX64MacArm(flutterEditionPath.resolve("3.47.5"));
    // the feeds also contain a beta release (3.49.0-0.1.pre) which must be filtered out, leaving only the stable version
    assertThat(flutterEditionPath.resolve("3.49.0-0.1.pre")).doesNotExist();
    // the feeds also contain a legacy v1.x release (v1.12.13+hotfix.9) which is below the minimum version and must be filtered out
    assertThat(flutterEditionPath.resolve("1.12.13+hotfix.9")).doesNotExist();
  }

  /**
   * Test of {@link FlutterUrlUpdater#mapVersion(String)}: the legacy {@code v1.x} releases are skipped (returning {@code null}) while a supported stable
   * version is returned normalized.
   */
  @Test
  void testFlutterUrlUpdaterMapVersion() {

    FlutterUrlUpdater updater = new FlutterUrlUpdater();

    // the legacy v1.x release is below the minimum version (3.0.0) and must be skipped
    assertThat(updater.mapVersion("v1.12.13+hotfix.9")).isNull();
    // a supported stable release is returned unchanged
    assertThat(updater.mapVersion("3.47.5")).isEqualTo("3.47.5");
  }
}
