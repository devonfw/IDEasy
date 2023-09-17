package com.devonfw.tools.ide.integrationtest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.UrlUpdater;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UrlUpdater} using wiremock to simulate network downloads.
 */
@WireMockTest(httpPort = 8080)
public class UrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String testdataRoot = "src/test/resources/integrationtest/UrlUpdaterTest";

  /**
   * Tests if the {@link com.devonfw.tools.ide.url.updater.UrlUpdater} can automatically add a missing OS (in this case
   * the linux_x64)
   *
   * @param tempDir Temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testUrlUpdaterMissingOsGetsAddedAutomatically(@TempDir Path tempDir) throws IOException {

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMock updater = new UrlUpdaterMock();

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
  public void testUrlUpdaterIsNotUpdatingWhenStatusManualIsTrue(@TempDir Path tempDir) throws IOException {

    // arrange
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UrlUpdaterMockSingle updater = new UrlUpdaterMockSingle();

    // act
    updater.update(urlRepository);
    Path versionsPath = Paths.get(testdataRoot).resolve("mocked").resolve("mocked").resolve("1.0");

    // assert
    assertThat(versionsPath.resolve("windows_x64.urls")).doesNotExist();
    assertThat(versionsPath.resolve("windows_x64.urls.sha256")).doesNotExist();

  }

  private static StatusJson getStatusJson(Path versionsPath) {

    ObjectMapper MAPPER = JsonMapping.create();
    StatusJson statusJson = new StatusJson();
    Path statusJsonPath = versionsPath.resolve("status.json");

    if (Files.exists(statusJsonPath)) {
      try (BufferedReader reader = Files.newBufferedReader(statusJsonPath)) {
        statusJson = MAPPER.readValue(reader, StatusJson.class);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + statusJsonPath, e);
      }
    }
    return statusJson;
  }
}
