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

  public boolean equalsReport(UrlUpdaterReport report1, UrlUpdaterReport report2) {

    return (report1.getAddVersionSuccess() == report2.getAddVersionSuccess()) && (report1.getAddVersionFailure() == report2.getAddVersionFailure())
        && (report1.getVerificationSuccess() == report2.getVerificationSuccess()) && (report1.getVerificationFailure() == report2.getVerificationFailure());
  }

  @Test
  public void testReportWithoutErrorsAndEmptyRepo(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // init test infrastructure
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);
    UrlFinalReport urlFinalReport = new UrlFinalReport();
    updater.setUrlFinalReport(urlFinalReport);
    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // first case: url downloads of versions of all os's works
    // assign
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    UrlUpdaterTestReport expectedReport = new UrlUpdaterTestReport("mocked", "mocked", 3, 0, 12, 0);
    // act
    updater.update(urlRepository);
    // assert
    assertThat(equalsReport(urlFinalReport.getUrlUpdaterReports().get(0), expectedReport)).isEqualTo(true);

    // second case: url downloads of existing versions and mac url doesn't work
    // assign
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    expectedReport = new UrlUpdaterTestReport("mocked", "mocked", 0, 0, 6, 6);
    // act
    updater.update(urlRepository);
    // assert
    assertThat(equalsReport(urlFinalReport.getUrlUpdaterReports().get(1), expectedReport)).isEqualTo(true);

    // third case: one version for Windows removed
    // assign
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    expectedReport = new UrlUpdaterTestReport("mocked", "mocked", 1, 0, 7, 8);
    // act
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("windows_x64.urls.sha256"));
    UrlRepository urlRepositoryWithError = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithError);
    // assert
    assertThat(equalsReport(urlFinalReport.getUrlUpdaterReports().get(2), expectedReport)).isEqualTo(true);

    // fourth case: one complete version removed and mac urls work and others don't work
    // assign
    stubFor(any(urlMatching("/os.*")).willReturn(aResponse().withStatus(400).withBody("aBody")));
    stubFor(any(urlMatching("/os/mac.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    expectedReport = new UrlUpdaterTestReport("mocked", "mocked", 1, 0, 6, 6);
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
    urlRepositoryWithError = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithError);
    // assert
    assertThat(equalsReport(urlFinalReport.getUrlUpdaterReports().get(3), expectedReport)).isEqualTo(true);

  }

}

