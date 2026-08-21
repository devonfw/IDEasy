package com.devonfw.tools.ide.url.updater;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlChecksum;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.status.StatusJson;
import com.devonfw.tools.ide.url.updater.status.UrlStatus;
import com.devonfw.tools.ide.url.updater.status.UrlStatusFile;
import com.devonfw.tools.ide.url.updater.status.UrlStatusState;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UrlUpdater}.
 */
@WireMockTest
class UrlUpdaterTest extends AbstractUrlUpdaterTest {

  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/UrlUpdaterTest";

  /**
   * Tests if the {@link UrlUpdater} can automatically add a missing OS (in this case the linux_x64)
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails
   */
  @Test
  void testUrlUpdaterMissingOsGetsAddedAutomatically(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);

    // when
    update(updater, urlRepository);

    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // then
    assertThat(versionsPath.resolve("status.json")).exists();
    assertThat(versionsPath.resolve("linux_x64.urls")).exists();
    assertThat(versionsPath.resolve("linux_x64.urls.sha256")).exists();
    assertThat(versionsPath.resolve("mac_arm64.urls")).exists();
    assertThat(versionsPath.resolve("mac_arm64.urls.sha256")).exists();
    assertThat(versionsPath.resolve("mac_x64.urls")).exists();
    assertThat(versionsPath.resolve("mac_x64.urls.sha256")).exists();
    assertThat(versionsPath.resolve("windows_x64.urls")).exists();
    assertThat(versionsPath.resolve("windows_x64.urls.sha256")).exists();

    Files.deleteIfExists(versionsPath.resolve("linux_x64.urls"));
    Files.deleteIfExists(versionsPath.resolve("linux_x64.urls.sha256"));

    // re-initialize UrlRepository
    UrlRepository urlRepositoryNew = UrlRepository.load(tempDir);
    update(updater, urlRepositoryNew);

    assertThat(versionsPath.resolve("linux_x64.urls")).exists();
    assertThat(versionsPath.resolve("linux_x64.urls.sha256")).exists();

  }

  @Test
  void testUrlUpdaterIsNotUpdatingWhenStatusManualIsTrue(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);

    // act
    update(updater, urlRepository);
    Path versionsPath = Path.of(TEST_DATA_ROOT).resolve("mocked").resolve("mocked").resolve("1.0");

    // assert
    assertThat(versionsPath.resolve("windows_x64.urls")).doesNotExist();
    assertThat(versionsPath.resolve("windows_x64.urls.sha256")).doesNotExist();

  }

  /**
   * Tests if the timestamps of the status.json get updated properly if a first error is detected.
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testStatusJsonUpdateOnFirstError(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    String toolName = "mocked";
    String editionName = "mocked";
    String versionName = "1.0";
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz";
    Instant now = Instant.now();
    Instant lastMonth = now.minus(31, ChronoUnit.DAYS);
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlTool urlTool = urlRepository.getOrCreateChild(toolName);
    UrlEdition urlEdition = urlTool.getOrCreateChild(editionName);
    UrlVersion urlVersion = urlEdition.getOrCreateChild(versionName);
    // we create the structure of our tool version and URL to simulate it was valid last moth
    Path statusPath = urlVersion.getPath().resolve(UrlStatusFile.STATUS_JSON);
    UrlStatusFile statusFile = new UrlStatusFile(statusPath);
    UrlStatus status = statusFile.getStatusJson().getOrCreateUrlStatus(url);
    UrlStatusState successState = new UrlStatusState(lastMonth); // ensure that we trigger a recheck of the URL
    status.setSuccess(successState);
    statusFile.markModified();
    UrlDownloadFile urlDownloadFile = urlVersion.getOrCreateUrls(OperatingSystem.WINDOWS, SystemArchitecture.X64);
    urlDownloadFile.addUrl(url);
    UrlChecksum urlChecksum = urlVersion.getOrCreateChecksum(urlDownloadFile.getName());
    urlChecksum.setChecksum("1234567890");
    urlVersion.save();
    statusFile.save();
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);
    // now we want to simulate that the url got broken (404) and the updater is properly handling this
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(404)));

    // act
    update(updater, urlRepository);

    // assert
    StatusJson statusJson = retrieveStatusJson(urlRepository, toolName, editionName, versionName);
    status = statusJson.getStatus(url);
    successState = status.getSuccess();
    assertThat(successState).isNotNull();
    assertThat(successState.getTimestamp()).isEqualTo(lastMonth);
    UrlStatusState errorState = status.getError();
    assertThat(errorState).isNotNull();
    assertThat(errorState.getCode()).isEqualTo(404);
    assertThat(Duration.between(errorState.getTimestamp(), now)).isLessThan(Duration.ofSeconds(5));
  }

  /**
   * Tests if the timestamps of the status.json get updated properly on success after an error.
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSuccessStateUpdatedAfterError(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    String toolName = "mocked";
    String editionName = "mocked";
    String versionName = "1.0";
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz";
    Instant now = Instant.now();
    Instant lastMonth = now.minus(31, ChronoUnit.DAYS);
    Instant lastSuccess = lastMonth.minus(1, ChronoUnit.DAYS);
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlTool urlTool = urlRepository.getOrCreateChild(toolName);
    UrlEdition urlEdition = urlTool.getOrCreateChild(editionName);
    UrlVersion urlVersion = urlEdition.getOrCreateChild(versionName);
    // we create the structure of our tool version and URL to simulate it was valid last moth
    Path statusPath = urlVersion.getPath().resolve(UrlStatusFile.STATUS_JSON);
    UrlStatusFile statusFile = new UrlStatusFile(statusPath);
    UrlStatus status = statusFile.getStatusJson().getOrCreateUrlStatus(url);
    UrlStatusState successState = new UrlStatusState(lastSuccess);
    status.setSuccess(successState);
    UrlStatusState errorState = new UrlStatusState(lastMonth);
    errorState.setCode(404);
    status.setError(errorState);
    statusFile.markModified();
    UrlDownloadFile urlDownloadFile = urlVersion.getOrCreateUrls(OperatingSystem.WINDOWS, SystemArchitecture.X64);
    urlDownloadFile.addUrl(url);
    UrlChecksum urlChecksum = urlVersion.getOrCreateChecksum(urlDownloadFile.getName());
    urlChecksum.setChecksum("1234567890");
    urlVersion.save();
    statusFile.save();
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);
    // now we want to simulate that the broken url is working again
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));

    // act
    update(updater, urlRepository);

    // assert
    status = retrieveStatusJson(urlRepository, toolName, editionName, versionName).getStatus(url);
    successState = status.getSuccess();
    assertThat(successState).isNotNull();
    assertThat(Duration.between(successState.getTimestamp(), now)).isLessThan(Duration.ofSeconds(5));
    errorState = status.getError();
    assertThat(errorState).isNotNull();
    assertThat(errorState.getCode()).isEqualTo(404);
    assertThat(errorState.getTimestamp()).isEqualTo(lastMonth);
  }

  /**
   * Tests if the tool version gets entirely removed if all versions are broken for a long time.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testVersionRemovedIfErrorPersists(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    String toolName = "mocked";
    String editionName = "mocked";
    String versionName = "1.0";
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz";
    Instant now = Instant.now();
    Instant lastMonth = now.minus(31, ChronoUnit.DAYS);
    Instant lastSuccess = lastMonth.minus(1, ChronoUnit.DAYS);
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlTool urlTool = urlRepository.getOrCreateChild(toolName);
    UrlEdition urlEdition = urlTool.getOrCreateChild(editionName);
    UrlVersion urlVersion = urlEdition.getOrCreateChild(versionName);
    // we create the structure of our tool version and URL to simulate it was valid last moth
    Path statusPath = urlVersion.getPath().resolve(UrlStatusFile.STATUS_JSON);
    UrlStatusFile statusFile = new UrlStatusFile(statusPath);
    UrlStatus status = statusFile.getStatusJson().getOrCreateUrlStatus(url);
    UrlStatusState successState = new UrlStatusState(lastSuccess);
    status.setSuccess(successState);
    UrlStatusState errorState = new UrlStatusState(lastMonth);
    errorState.setCode(404);
    status.setError(errorState);
    statusFile.markModified();
    UrlDownloadFile urlDownloadFile = urlVersion.getOrCreateUrls(OperatingSystem.WINDOWS, SystemArchitecture.X64);
    urlDownloadFile.addUrl(url);
    UrlChecksum urlChecksum = urlVersion.getOrCreateChecksum(urlDownloadFile.getName());
    urlChecksum.setChecksum("1234567890");
    urlVersion.save();
    statusFile.save();
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);
    // now we want to simulate that the url got broken (404) and the updater is properly handling this
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(404)));

    // act
    update(updater, urlRepository);

    // assert
    assertThat(urlVersion.getPath()).doesNotExist();
  }

  /**
   * Tests if the {@link UrlUpdater} will fail resolving a server with a Content-Type:text header response.
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testUrlUpdaterWithTextContentTypeWillNotCreateStatusJson(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);

    // when
    update(updater, urlRepository);

    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // then
    assertThat(versionsPath.resolve("status.json")).doesNotExist();

  }

  /**
   * Tests if the {@link UrlUpdater} will handle the literally latest version of a tool correctly
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testUrlUpdaterWithOnlyLatestVersion(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    //given
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);
    updater.setVersion("latest");

    // when
    update(updater, urlRepository);

    // then
    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("latest");
    assertThat(versionsPath.resolve("status.json")).exists();
  }

}
