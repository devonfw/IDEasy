package com.devonfw.tools.ide.url.tool.spyder;

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

@WireMockTest
public class SpyderUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testSpyderUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/spyder-ide/spyder/git/refs/tags"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("SpyderUrlUpdater")
                .resolve("spyder-tags.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/spyder-ide/spyder/releases/download/v.*/Spyder-[a-zA-Z0-9._-]+\\.(exe|sh|pkg)"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SpyderUrlUpdaterMock updater = new SpyderUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path spyderDir = tempDir.resolve("spyder").resolve("spyder");
    assertUrlVersionOsX64MacArm(spyderDir.resolve("6.1.4"));
    assertUrlVersionOsX64MacArm(spyderDir.resolve("6.0.1"));
  }
}
