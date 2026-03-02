package com.devonfw.tools.ide.url.tool.java;

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
 * Test of {@link JavaUrlUpdater}.
 */
@WireMockTest
class JavaUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link JavaUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testJavaUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    stubFor(get(urlMatching("/versions/")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("JavaUrlUpdater").resolve("java-version.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching(
        "/temurin[0-9]*-binaries/releases/download/jdk-[0-9A-Z.%]+/OpenJDK[0-9U]*-jdk_(x64|aarch64)_(windows|linux|mac)_hotspot_[0-9._]+\\.(zip|tar\\.gz)")).willReturn(
        aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    JavaUrlUpdaterMock updater = new JavaUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path javaEditionPath = tempDir.resolve("java").resolve("java");

    // then
    assertUrlVersionOsX64MacArm(javaEditionPath.resolve("21.0.3_9"));
    assertUrlVersionOsX64MacArm(javaEditionPath.resolve("22.0.1_8"));
    assertUrlVersionOsX64MacArm(javaEditionPath.resolve("22_36"));
  }

}
