package com.devonfw.tools.ide.url.report;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.tool.AbstractUrlUpdaterTest;
import com.devonfw.tools.ide.tool.UrlUpdaterMock;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UrlUpdaterReport} and {@link UrlFinalReport} using wiremock to simulate network downloads.
 */
@WireMockTest
public class UrlUpdaterReportTest extends AbstractUrlUpdaterTest {

  /**
   * Tests for {@link UrlUpdaterReport} if information of updaters is collected correctly
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails
   */
  @Test
  public void testReportWithoutErrorsAndEmptyRepo(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

    // assign
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    UrlUpdaterReport expectedReport = new UrlUpdaterReport("mocked", "mocked", 3, 0, 12, 0);
    // act
    updater.update(urlRepository);
    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports().get(0)).isEqualTo(expectedReport);

  }

  /**
   * Test with existing versions and not empty repo and some failing urls
   */
  @Test
  public void testReportWithExistVersionsAndFailedDownloads(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // pre configuration
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    updater.update(urlRepository);
    // assign
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    UrlUpdaterReport expectedReport = new UrlUpdaterReport("mocked", "mocked", 0, 0, 6, 6);
    // act
    updater.update(urlRepository);
    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports().get(1)).isEqualTo(expectedReport);
  }

  /**
   * Test one os version url file is removed in already existing repository
   */
  @Test
  public void testReportAfterVersionForOsRemoved(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // pre configuration
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    updater.update(urlRepository);
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    updater.update(urlRepository);
    // assign
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    UrlUpdaterReport expectedReport = new UrlUpdaterReport("mocked", "mocked", 1, 0, 7, 8);
    // act
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls.sha256"));
    UrlRepository urlRepositoryWithError = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithError);
    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports().get(2)).isEqualTo(expectedReport);
  }

  /**
   * Test after one version is completely removed in already existing repository and download urls response is reversed
   */
  @Test
  public void testReportAfterVersionsForRemovedAndReversedUrlResponses(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // pre configuration
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    updater.update(urlRepository);
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    updater.update(urlRepository);
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    updater.update(urlRepository);
    // assign
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    UrlUpdaterReport expectedReport = new UrlUpdaterReport("mocked", "mocked", 1, 0, 6, 6);
    // act
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls.sha256"));
    Files.deleteIfExists(versionsPath.resolve("mac_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("mac_x64.urls.sha256"));
    Files.deleteIfExists(versionsPath.resolve("mac_arm64.urls"));
    Files.deleteIfExists(versionsPath.resolve("mac_arm64.urls.sha256"));
    Files.deleteIfExists(versionsPath.resolve("linux_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("linux_x64.urls.sha256"));
    Files.deleteIfExists(versionsPath.resolve("status.json"));
    Files.deleteIfExists(versionsPath);
    UrlRepository urlRepositoryWithError = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithError);
    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports().get(3)).isEqualTo(expectedReport);

  }

}

