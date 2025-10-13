package com.devonfw.tools.ide.context;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.repository.MvnArtifactMetadata;
import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock class for {@link MvnRepository}.
 */
public class MvnRepositoryMock extends MvnRepository {

  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   * @param wmRuntimeInfo wireMock server on a random port.
   */
  public MvnRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {
    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  public Path download(MvnArtifactMetadata metadata) {
    MvnArtifact artifact = metadata.getMvnArtifact();
    String path = artifact.getDownloadUrl();
    String url = this.wmRuntimeInfo.getHttpBaseUrl() + path;
    Path archiveFolder = context.getIdeRoot().resolve("repository").resolve("mvn").resolve(artifact.getGroupId() + "/" + artifact.getArtifactId());
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
      context.getFileAccess().compress(archiveFolder, baos, artifact.getFilename());
      byte[] body = baos.toByteArray();
      stubFor(get(urlPathEqualTo(path)).willReturn(
          aResponse().withStatus(200).withBody(body)));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create mock archive for " + url, e);
    }
    return archiveFolder;
  }
}
