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

import com.devonfw.tools.ide.tool.uv.UvRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/** Mock class for {@link UvRepository}. */
public class UvRepositoryMock extends UvRepository {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param wmRuntimeInfo wireMock server on a random part
   */
  public UvRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {

    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
    mockPypiPackageResponses(wmRuntimeInfo);
  }

  @Override
  protected String getRegistryUrl() {

    return wmRuntimeInfo.getHttpBaseUrl() + "/";
  }

  /**
   * Creates PyPI JSON responses based on the given test resources.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo} providing the base URL.
   */
  private void mockPypiPackageResponses(WireMockRuntimeInfo wireMockRuntimeInfo) {
    Path pypiRoot = this.context.getIdeHome().getParent().resolve("repository").resolve("pypi");
    if (!Files.isDirectory(pypiRoot)) {
      return;
    }
    try (Stream<Path> files = Files.walk(pypiRoot)) {
      files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
          .forEach(jsonFile -> {
            Path rel = pypiRoot.relativize(jsonFile);
            String packageName = rel.toString().replace(File.separatorChar, '/').replaceAll("\\.json$", "");
            String packagePath = "/" + packageName + "/json";
            String body = IdeTestContext.readAndResolveBaseUrl(jsonFile, wireMockRuntimeInfo);
            stubFor(get(urlPathEqualTo(packagePath))
                .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody(body)));
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
