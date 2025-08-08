package com.devonfw.tools.ide.url.tool.terraform;

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
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link TerraformUrlUpdater}.
 */
@WireMockTest
class TerraformUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for TerraformUrlUpdater: verifies that update creates expected files for Terraform versions.
   */
  @Test
  void testTerraformUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/hashicorp/terraform/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("TerraformUrlUpdater").resolve("terraform-tags.json"), wmRuntimeInfo))));
    stubFor(
        any(urlMatching("/terraform/.*/terraform_.*_(linux|windows|darwin)_(amd|arm)64\\.zip")).willReturn(
            aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    TerraformUrlUpdaterMock updater = new TerraformUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path terraformEditionPath = tempDir.resolve("terraform").resolve("terraform");
    for (String version : new String[] { "1.5.0", "1.6.0" }) {
      Path terraformVersionPath = terraformEditionPath.resolve(version);
      assertUrlVersionOsX64MacArm(terraformVersionPath);
    }
  }
}

