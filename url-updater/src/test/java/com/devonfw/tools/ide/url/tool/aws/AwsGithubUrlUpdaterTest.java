package com.devonfw.tools.ide.url.tool.aws;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link AwsUrlUpdater}
 */
@WireMockTest
public class AwsGithubUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/AwsUrlUpdater";

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link AwsUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testAwsGithubUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/.*")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("AwsUrlUpdater").resolve("github-tags.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/download/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AwsGithubUrlUpdaterMock updater = new AwsGithubUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path awsEditionDir = tempDir.resolve("aws").resolve("aws");
    for (String version : new String[] { "2.7.22", "2.7.23", "2.7.24" }) {
      assertUrlVersionOsX64(awsEditionDir.resolve(version));
    }
  }

}
