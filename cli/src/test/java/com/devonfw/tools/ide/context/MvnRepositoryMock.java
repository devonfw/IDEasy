package com.devonfw.tools.ide.context;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.repository.MvnArtifactMetadata;
import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
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
    mockMvnMetadataResponses(wmRuntimeInfo);
  }

  @Override
  public Path download(MvnArtifactMetadata metadata) {
    MvnArtifact artifact = metadata.getMvnArtifact();
    String path = artifact.getDownloadUrl();
    path = path.replace(MvnRepositoryMock.MAVEN_CENTRAL, "");
    String url = this.wmRuntimeInfo.getHttpBaseUrl() + path;
    Path archiveFolder = this.context.getIdeRoot().resolve("repository").resolve("mvn").resolve(artifact.getGroupId() + "/" + artifact.getArtifactId());
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
      this.context.getFileAccess().compress(archiveFolder, baos, artifact.getFilename());
      byte[] body = baos.toByteArray();
      stubFor(get(urlPathEqualTo(path)).willReturn(
          aResponse().withStatus(200).withBody(body)));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create mock archive for " + url, e);
    }
    return super.download(metadata);
  }

  @Override
  protected UrlChecksums getChecksums(MvnArtifact artifact) {
    return null;
  }

  @Override
  protected String getMavenUrl(MvnArtifact artifact) {
    return artifact.getDownloadUrl().replace(MvnRepository.MAVEN_CENTRAL, this.wmRuntimeInfo.getHttpBaseUrl());
  }

  private void mockMvnMetadataResponses(WireMockRuntimeInfo wireMockRuntimeInfo) {
    Path mvnRoot = this.context.getIdeHome()
        .getParent()
        .resolve("repository")
        .resolve("mvn");

    try (Stream<Path> files = Files.walk(mvnRoot)) {
      files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".xml"))
          .forEach(xmlFile -> {
            // Derive package path from relative file path, e.g.
            //   <root>/org/springframework/boot/maven-metadata.xml  -> org/springframework/boot
            Path rel = mvnRoot.relativize(xmlFile);
            String packagePath = "/" + rel.toString()
                .replace(File.separatorChar, '/');

            String body = IdeTestContext.readAndResolveBaseUrl(xmlFile, wireMockRuntimeInfo);

            stubFor(get(urlPathEqualTo(packagePath))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(body)));
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
