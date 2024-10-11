package com.devonfw.tools.ide.tool;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.file.json.UrlStatus;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link com.devonfw.tools.ide.url.updater.UrlUpdater} using wiremock to simulate network downloads.
 */
@WireMockTest
public class UrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/UrlUpdaterTest";

  /**
   * Tests if the {@link com.devonfw.tools.ide.url.updater.UrlUpdater} can automatically add a missing OS (in this case the linux_x64)
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails
   */
  @Test
  public void testUrlUpdaterMissingOsGetsAddedAutomatically(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

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
    updater.update(urlRepositoryNew);

    assertThat(versionsPath.resolve("linux_x64.urls")).exists();
    assertThat(versionsPath.resolve("linux_x64.urls.sha256")).exists();

  }

  @Test
  public void testUrlUpdaterIsNotUpdatingWhenStatusManualIsTrue(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);

    // act
    updater.update(urlRepository);
    Path versionsPath = Path.of(TEST_DATA_ROOT).resolve("mocked").resolve("mocked").resolve("1.0");

    // assert
    assertThat(versionsPath.resolve("windows_x64.urls")).doesNotExist();
    assertThat(versionsPath.resolve("windows_x64.urls.sha256")).doesNotExist();

  }

  /**
   * Tests if the timestamps of the status.json get updated properly. Creates an initial status.json with a success timestamp. Updates the status.json with an
   * error timestamp and compares it with the success timestamp. Updates the status.json with a final success timestamp and compares it with the error
   * timestamp.
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testUrlUpdaterStatusJsonRefreshBugStillExisting(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);

    String statusUrl = wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz";
    String toolName = "mocked";
    String editionName = "mocked";
    String versionName = "1.0";

    // act
    updater.update(urlRepository);

    Path versionsPath = tempDir.resolve(toolName).resolve(editionName).resolve(versionName);

    // assert
    assertThat(versionsPath.resolve("status.json")).exists();

    StatusJson statusJson = retrieveStatusJson(urlRepository, toolName, editionName, versionName);

    UrlStatus urlStatus = statusJson.getOrCreateUrlStatus(statusUrl);

    Instant successTimestamp = urlStatus.getSuccess().getTimestamp();

    assertThat(successTimestamp).isNotNull();

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(404)));

    // re-initialize UrlRepository for error timestamp
    UrlRepository urlRepositoryWithError = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithError);

    statusJson = retrieveStatusJson(urlRepositoryWithError, toolName, editionName, versionName);

    urlStatus = statusJson.getOrCreateUrlStatus(statusUrl);
    successTimestamp = urlStatus.getSuccess().getTimestamp();
    Instant errorTimestamp = urlStatus.getError().getTimestamp();
    Integer errorCode = urlStatus.getError().getCode();

    assertThat(errorCode).isEqualTo(404);
    assertThat(errorTimestamp).isAfter(successTimestamp);

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));

    // re-initialize UrlRepository for error timestamp
    UrlRepository urlRepositoryWithSuccess = UrlRepository.load(tempDir);
    updater.update(urlRepositoryWithSuccess);

    assertThat(versionsPath.resolve("status.json")).exists();

    statusJson = retrieveStatusJson(urlRepositoryWithSuccess, toolName, editionName, versionName);

    urlStatus = statusJson.getOrCreateUrlStatus(statusUrl);

    successTimestamp = urlStatus.getSuccess().getTimestamp();
    errorTimestamp = urlStatus.getError().getTimestamp();
    errorCode = urlStatus.getError().getCode();

    assertThat(errorCode).isEqualTo(200);
    assertThat(errorTimestamp).isAfter(successTimestamp);

  }

  /**
   * Tests if the {@link com.devonfw.tools.ide.url.updater.UrlUpdater} will fail resolving a server with a Content-Type:text header response.
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param tempDir Temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testUrlUpdaterWithTextContentTypeWillNotCreateStatusJson(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path versionsPath = tempDir.resolve("mocked").resolve("mocked").resolve("1.0");

    // then
    assertThat(versionsPath.resolve("status.json")).doesNotExist();

  }

}
