package com.devonfw.tools.ide.context;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.devonfw.tools.ide.tool.repository.NpmRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock class for {@link NpmRepository}.
 */
public class NpmRepositoryMock extends NpmRepository {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  public NpmRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {

    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
    mockNpmPackageResponses(wmRuntimeInfo);
  }

  @Override
  public String getRegistryUrl() {

    return wmRuntimeInfo.getHttpBaseUrl() + "/";
  }

  /**
   * Creates a npm-version json file based on the given test resource in a temporary directory according to the http url and port of the
   * {@link WireMockRuntimeInfo}.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo} providing the base URL.
   */
  private void mockNpmPackageResponses(WireMockRuntimeInfo wireMockRuntimeInfo) {
    Path npmRoot = this.context.getIdeHome()
        .getParent()
        .resolve("repository")
        .resolve("npmjs");

    try (Stream<Path> files = Files.walk(npmRoot)) {
      files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
          .forEach(jsonFile -> {
            // Derive package path from relative file path, e.g.
            //   <root>/@angular/cli.json  -> "/@angular/cli"
            //   <root>/yarn.json          -> "/yarn"
            Path rel = npmRoot.relativize(jsonFile);
            String packagePath = "/" + rel.toString()
                .replace(File.separatorChar, '/')
                .replaceAll("\\.json$", "");

            String body = IdeTestContext.readAndResolveBaseUrl(jsonFile, wireMockRuntimeInfo);

            stubFor(get(urlPathEqualTo(packagePath))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)));

          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
