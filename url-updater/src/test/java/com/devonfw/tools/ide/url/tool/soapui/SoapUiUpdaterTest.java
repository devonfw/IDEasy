package com.devonfw.tools.ide.url.tool.soapui;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Tests for (@link SoapUiUrlUpdater}.
 */
@WireMockTest
public class SoapUiUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link SoapUiUrlUpdater} creating the per-OS/arch download URLs and checksums.
   *
   * @param tempDir temporary directory to use.
   * @param wmRuntimeInfo the {@link com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo}.
   */
  @Test
  void testSoapUiUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    //arrange
    stubFor(get(urlMatching("/repos/SmartBear/soapui/releases"))
        .willReturn(aResponse().withStatus(200).withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("SoapUiUpdater")
            .resolve("soapui-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/soapuios/.*/SoapUI-.*-(windows-bin\\.zip|linux-bin\\.tar\\.gz|mac-x64-bin\\.zip|mac-arm64-bin\\.zip)"))
        .willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SoapUiUrlUpdater updater = new SoapUiUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    //act
    updater.update(urlRepository);

    //assert
    Path soapUiVersionDir = tempDir.resolve("soapui").resolve("soapui");
    assertUrlVersionOsX64MacArm(soapUiVersionDir.resolve("5.10.0"));
    assertUrlVersionOsX64MacArm(soapUiVersionDir.resolve("5.8.0"));
  }

}
