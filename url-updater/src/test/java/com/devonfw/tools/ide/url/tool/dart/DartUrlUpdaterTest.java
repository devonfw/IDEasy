package com.devonfw.tools.ide.url.tool.dart;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link DartUrlUpdater}.
 */
@WireMockTest
class DartUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testDartUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    Path testDir = PATH_INTEGRATION_TEST.resolve("DartUrlUpdater");
    // the objects list API that enumerates the stable release folders (urlPathEqualTo ignores the query string)
    stubFor(get(urlPathEqualTo("/storage/v1/b/dart-archive/o")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(testDir.resolve("release-list.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/channels/stable/release/[\\d.]+/sdk/dartsdk-(linux|macos|windows)-(x64|arm64)-release\\.zip")).willReturn(
        aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    DartUrlUpdater updater = new DartUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());
    // when
    update(updater, urlRepository);

    // then
    Path dartEditionPath = tempDir.resolve("dart").resolve("dart");
    // every supported stable release of the listing is recorded, so DART_VERSION can be pinned
    assertUrlVersionDart(dartEditionPath.resolve("3.0.0"));
    assertUrlVersionDart(dartEditionPath.resolve("3.13.4"));
    assertUrlVersionDart(dartEditionPath.resolve("3.13.5"));
    // the "latest" alias is not a concrete release and must not be recorded
    assertThat(dartEditionPath.resolve("latest")).doesNotExist();
    // releases below the minimum version (3.0.0) have no macos-arm64 archive and must be filtered out
    assertThat(dartEditionPath.resolve("1.11.0")).doesNotExist();
    assertThat(dartEditionPath.resolve("2.10.5")).doesNotExist();
  }

  /**
   * Test of {@link DartUrlUpdater#mapVersion(String)}: the {@code latest} alias and every release below the minimum version {@code 3.0.0} are skipped
   * (returning {@code null}) while a supported stable release is returned unchanged.
   */
  @Test
  void testDartUrlUpdaterMapVersion() {

    DartUrlUpdater updater = new DartUrlUpdater();

    // the "latest" alias is not a concrete release and must be skipped
    assertThat(updater.mapVersion("latest")).isNull();
    // a release below the minimum version has no macos-arm64 archive and must be skipped
    assertThat(updater.mapVersion("2.10.5")).isNull();
    // the minimum version itself and a newer release are returned unchanged
    assertThat(updater.mapVersion("3.0.0")).isEqualTo("3.0.0");
    assertThat(updater.mapVersion("3.13.5")).isEqualTo("3.13.5");
  }
}
