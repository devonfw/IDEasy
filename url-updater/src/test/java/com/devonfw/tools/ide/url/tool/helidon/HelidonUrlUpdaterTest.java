package com.devonfw.tools.ide.url.tool.helidon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link HelidonUrlUpdater}.
 */
@WireMockTest
public class HelidonUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link HelidonUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testHelidonUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    stubFor(get(urlMatching("/io/helidon/build-tools/cli/helidon-cli-impl/maven-metadata.xml"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("HelidonUrlUpdater")
                .resolve("helidon-cli-impl-maven-metadata.xml"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/io/helidon/build-tools/cli/helidon-cli-impl/.*\\.zip"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/zip")
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    HelidonUrlUpdater updater = new HelidonUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    // the distribution is a single OS-agnostic zip, so a generic "urls" file is expected (not per-OS files)
    Path helidonEditionDir = tempDir.resolve("helidon").resolve("helidon");
    assertUrlVersionAgnostic(helidonEditionDir.resolve("4.0.30"));
    assertUrlVersionAgnostic(helidonEditionDir.resolve("4.0.0"));

    assertThat(helidonEditionDir.resolve("4.0.30").resolve("urls"))
        .content()
        .contains(wmRuntimeInfo.getHttpBaseUrl() + "/io/helidon/build-tools/cli/helidon-cli-impl/4.0.30/helidon-cli-impl-4.0.30.zip");

    // only the 4.x line is tracked, older lines are filtered out
    assertThat(helidonEditionDir.resolve("3.0.6")).doesNotExist();
    assertThat(helidonEditionDir.resolve("2.3.3")).doesNotExist();
  }
}
