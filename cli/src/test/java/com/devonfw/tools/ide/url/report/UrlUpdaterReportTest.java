package com.devonfw.tools.ide.url.report;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
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

  private UrlRepository urlRepository;
  private UrlUpdaterMock updater;

  /**
   * Configure {@link UrlRepository} and {@link UrlUpdaterMock} before each test
   *
   * @param tempDir temporary directory to use
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @BeforeEach
  public void setup(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    urlRepository = UrlRepository.load(tempDir);
    updater = new UrlUpdaterMock(wmRuntimeInfo);
  }

  /**
   * Test report on the first (initial) run when all URLs are successful
   */
  @Test
  public void testReportOnInitialRunWithAllUrlsSuccessful() {

    // arrange
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubSuccessfulUrlRequest();
    // 3 versions x 4 urls --> 3 additions and 12 verifications
    UrlUpdaterReport expectedReport = createReport(3, 0, 12, 0);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on the first (initial) run when all URLs are failing
   */
  @Test
  public void testReportOnInitialRunWithAllUrlsFailing() {

    // assign
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubFailedUrlRequest();
    updater.update(urlRepository);
    UrlUpdaterReport expectedReport = createReport(3, 0, 0, 12);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on first (initial) run when urls for mac_x64 and mac_arm64 are failing
   */
  @Test
  public void testReportOnFirstRunWithFailedUrlsForMac() {

    // assign
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubSuccessfulUrlRequest();
    stubFailedUrlRequest("/os/mac.*");
    updater.update(urlRepository);
    UrlUpdaterReport expectedReport = createReport(3, 0, 6, 6);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on second run with existing versions already verified within the timeframe
   */
  @Test
  public void testReportOnSecondRunWithExistVersionsAlreadyVerifiedInTime() {

    // arrange
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubSuccessfulUrlRequest();
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(0, 0, 0, 0);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on second run when existing versions re-verified after timeframe
   */
  @Test
  public void testReportOnSecondRunWithExistVersionsReVerifiedAfterTime() {

    // arrange
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubSuccessfulUrlRequest();
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(0, 0, 0, 0);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on second run when an url is removed from one version
   */
  @Test
  public void testReportOnSecondRunAfterOneVersionIsRemoved() throws IOException {

    // assign
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    stubSuccessfulUrlRequest();
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(1, 0, 4, 0);
    Path urlPath = urlRepository.getPath().resolve("mocked").resolve("mocked").resolve("1.0");
    Files.deleteIfExists(urlPath.resolve("windows_x64.urls"));
    Files.deleteIfExists(urlPath.resolve("windows_x64.urls.sha256"));
    urlRepository = UrlRepository.load(urlRepository.getPath());

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  // some utils
  private void stubSuccessfulUrlRequest() {

    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
  }

  private void stubFailedUrlRequest(String urlPattern) {

    stubFor(any(urlMatching(urlPattern)).willReturn(aResponse().withStatus(400).withBody("aBody")));
  }

  private void stubFailedUrlRequest() {

    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
  }

  private UrlUpdaterReport createReport(int addSucc, int addFail, int verSucc, int verFail) {

    return new UrlUpdaterReport("mocked", "mocked", addSucc, addFail, verSucc, verFail);
  }

}
