package com.devonfw.tools.ide.url.tool.squirrelsql;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Tests for {@link SquirrelSqlUrlUpdater}.
 */
@WireMockTest
public class SquirrelSqlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link SquirrelSqlUrlUpdater} for the creation of {@link SquirrelSqlUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testSquirrelSqlUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/squirrel-sql-client/squirrel-sql-stable-releases/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("SquirrelSqlUpdater")
                .resolve("squirrel-sql-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/*.*.*-a_plainzip/squirrelsql-*.*.*-optional.zip"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SquirrelSqlUrlUpdaterMock updater = new SquirrelSqlUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path squirrelSqlDir = tempDir.resolve("squirrelsql").resolve("squirrelsql");
    assertUrlVersion(squirrelSqlDir.resolve("5.1.0"), List.of(""));
    assertUrlVersion(squirrelSqlDir.resolve("4.4.0"), List.of(""));
  }
}
