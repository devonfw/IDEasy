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

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.devonfw.tools.ide.url.updater.UrlUpdaterMock;
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

    // assign
    stubSuccessfulUrlRequest();
    // 3 versions x 4 urls --> 3 additions and 12 verifications
    UrlUpdaterReport expectedReport = createReport(3, 0, 12, 0);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

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
    stubFailedUrlRequest();
    UrlUpdaterReport expectedReport = createReport(3, 0, 0, 12);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report on first (initial) run when urls for mac_x64 and mac_arm64 are failing
   */
  @Test
  public void testReportOnInitialRunWithFailedUrlsForMac() {

    // assign
    stubSuccessfulUrlRequest();
    stubFailedUrlRequest("/os/mac.*");
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(3, 0, 0, 6);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

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

    // assign
    stubSuccessfulUrlRequest();
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(0, 0, 0, 0);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

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
    stubSuccessfulUrlRequest();
    updater.update(urlRepository); // init successful update
    UrlUpdaterReport expectedReport = createReport(1, 0, 1, 0);
    Path urlPath = urlRepository.getPath().resolve("mocked").resolve("mocked").resolve("1.0");
    Files.deleteIfExists(urlPath.resolve("windows_x64.urls"));
    Files.deleteIfExists(urlPath.resolve("windows_x64.urls.sha256"));
    urlRepository = UrlRepository.load(urlRepository.getPath());
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(urlFinalReport.getUrlUpdaterReports()).contains(expectedReport);
  }

  /**
   * Test report total additions and verifications operations
   */
  @Test
  public void testReportTotalAdditionsAndVerificationsOperations() {

    // assign
    int addVersionSuccess = 5;
    int addVersionFailure = 0;
    int addVerificationSuccess = 10;
    int addVerificationFailure = 10;
    UrlUpdaterReport report = createReport(addVersionSuccess, addVersionFailure, addVerificationSuccess, addVerificationFailure);

    // assert
    assertThat(report.getTotalAdditions()).isEqualTo(report.getAddVersionSuccess() + report.getAddVersionFailure());
    assertThat(report.getTotalVerificitations()).isEqualTo(report.getVerificationSuccess() + report.getVerificationFailure());
  }

  /**
   * Test report increment operations for additions and verifications
   */
  @Test
  public void testReportIncrementOperations() {

    // assign
    int addVersionSuccess = 5;
    int addVersionFailure = 0;
    int addVerificationSuccess = 10;
    int addVerificationFailure = 10;
    UrlUpdaterReport report = createReport(addVersionSuccess, addVersionFailure, addVerificationSuccess, addVerificationFailure);

    // act
    report.incrementAddVersionSuccess();
    report.incrementAddVersionFailure();
    report.incrementVerificationSuccess();
    report.incrementVerificationFailure();

    // assert
    assertThat(report.getAddVersionSuccess()).isEqualTo(addVersionSuccess + 1);
    assertThat(report.getAddVersionFailure()).isEqualTo(addVersionFailure + 1);
    assertThat(report.getVerificationSuccess()).isEqualTo(addVerificationSuccess + 1);
    assertThat(report.getVerificationFailure()).isEqualTo(addVerificationFailure + 1);
  }

  /**
   * Test report error rate operations for additions and verifications
   */
  @Test
  public void testReportErrorRateOperations() {

    // assign
    int addVersionSuccess = 20;
    int addVersionFailureNull = 0;
    int addVerificationSuccessNull = 0;
    int addVerificationFailure = 10;
    int addVersionFailureIncremented = 5; // for testing without null
    int addVerificationSuccessIncremented = 10; // for testing without null
    UrlUpdaterReport reportWithNull = createReport(addVersionSuccess, addVersionFailureNull, addVerificationSuccessNull, addVerificationFailure);
    UrlUpdaterReport reportWithoutNull = createReport(addVersionSuccess, addVersionFailureIncremented, addVerificationSuccessIncremented,
        addVerificationFailure);

    // act
    double errorRateWithNullAdd = reportWithNull.getErrorRateAdditions();
    double errorRateWithNullVer = reportWithNull.getErrorRateVerificiations();
    double errorRateWithoutNullAdd = reportWithoutNull.getErrorRateAdditions();
    double errorRateWithoutNullVer = reportWithoutNull.getErrorRateVerificiations();

    // assert (failures / total) * 100
    assertThat(errorRateWithNullAdd).isEqualTo(0.00);
    assertThat(errorRateWithNullVer).isEqualTo(0.00);
    assertThat(errorRateWithoutNullAdd).isEqualTo(20.0);
    assertThat(errorRateWithoutNullVer).isEqualTo(50.0);
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
