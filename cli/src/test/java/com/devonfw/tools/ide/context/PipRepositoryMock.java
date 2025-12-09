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

import com.devonfw.tools.ide.tool.repository.PipRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock class for {@link PipRepository}.
 */
public class PipRepositoryMock extends PipRepository {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  public PipRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {

    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
    mockPypiPackageResponses(wmRuntimeInfo);
  }

  @Override
  public String getRegistryUrl() {

    return wmRuntimeInfo.getHttpBaseUrl() + "/";
  }

  /**
   * Creates PyPI JSON responses based on the given test resource in a temporary directory according to the http url and port of the
   * {@link WireMockRuntimeInfo}.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo} providing the base URL.
   */
  private void mockPypiPackageResponses(WireMockRuntimeInfo wireMockRuntimeInfo) {
    Path pypiRoot = this.context.getIdeHome()
        .getParent()
        .resolve("repository")
        .resolve("pypi");

    if (!Files.isDirectory(pypiRoot)) {
      return;
    }

    try (Stream<Path> files = Files.walk(pypiRoot)) {
      files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
          .forEach(jsonFile -> {
            // Derive package path from relative file path, e.g.
            //   <root>/pip.json  -> "/pip/json"
            Path rel = pypiRoot.relativize(jsonFile);
            String packageName = rel.toString()
                .replace(File.separatorChar, '/')
                .replaceAll("\\.json$", "");
            String packagePath = "/" + packageName + "/json";

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
