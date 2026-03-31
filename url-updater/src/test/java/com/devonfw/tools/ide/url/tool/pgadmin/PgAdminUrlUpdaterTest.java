package com.devonfw.tools.ide.url.tool.pgadmin;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link PgAdminUrlUpdater}.
 */
@WireMockTest
class PgAdminUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for PgAdminUrlUpdater: verifies that update creates expected files for PgAdmin versions.
   */
  @Test
  void testPgAdminUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/ftp/pgadmin/pgadmin4/")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("PgAdminUrlUpdater").resolve("index.html"), wmRuntimeInfo))));
    stubFor(
        any(urlMatching("/pub/pgadmin/pgadmin4/v[0-9.]+/(windows|macos)/pgadmin4-[0-9.]+(\\.exe|-x64\\.exe|\\.dmg|-arm64\\.dmg|-x86_64\\.dmg)"))
            .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PgAdminUrlUpdaterMock updater = new PgAdminUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path pgadminEditionPath = tempDir.resolve("pgadmin").resolve("pgadmin");
    assertUrlVersion(pgadminEditionPath.resolve("9.6"), List.of("windows_x64", "mac_x64", "mac_arm64"));
    for (String version : new String[] { "2.1", "1.6" }) {
      Path pgadminVersionPath = pgadminEditionPath.resolve(version);
      assertUrlVersion(pgadminVersionPath, List.of("windows_x64", "mac_x64"));
    }
  }
}

