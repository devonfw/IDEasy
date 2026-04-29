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
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnArtifactMetadata;
import com.devonfw.tools.ide.tool.mvn.MvnRepository;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock class for {@link MvnRepository}.
 */
public class MvnRepositoryMock extends MvnRepository {

  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * Maps artifact download path to its pre-computed SHA-256 checksum.
   * Populated when the mock archive is compressed, queried during verification.
   */
  private final Map<String, String> checksumByPath = new HashMap<>();

  /**
   * Registers an expected SHA-256 checksum for a given artifact path.
   *
   * @param path the artifact path (relative to repo root, e.g. "/org/apache/maven/apache-maven/3.8.1/apache-maven-3.8.1-bin.zip").
   * @param sha256 the expected SHA-256 hash.
   */
  public void putExpectedChecksum(String path, String sha256) {
    this.checksumByPath.put(path, sha256);
  }

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
      // Pre-compute and store the SHA-256 of the archive bytes so verifyChecksum() can check them.
      // We use putIfAbsent so that a hardcoded 'expected' checksum from a test takes precedence.
      String sha256 = sha256Hex(body);
      this.checksumByPath.putIfAbsent(path, sha256);
      stubFor(get(urlPathEqualTo(path)).willReturn(
          aResponse().withStatus(200).withBody(body)));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create mock archive for " + url, e);
    }
    return super.download(metadata);
  }

  @Override
  public UrlChecksums getChecksums(MvnArtifact artifact) {
    String path = artifact.getDownloadUrl().replace(MvnRepositoryMock.MAVEN_CENTRAL, "");
    String sha256 = this.checksumByPath.get(path);
    if (sha256 == null) {
      // checksum not yet computed (e.g. metadata requests) – skip verification
      return null;
    }
    return new SingleChecksumWrapper(sha256);
  }

  private static String sha256Hex(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data);
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Simple {@link UrlChecksums} wrapper for a single pre-computed checksum.
   */
  private record SingleChecksumWrapper(String sha256) implements UrlChecksums {

    @Override
    public Iterator<UrlGenericChecksum> iterator() {
      UrlGenericChecksum entry = new UrlGenericChecksum() {
        @Override public String getChecksum() { return sha256; }
        @Override public String getHashAlgorithm() { return "SHA-256"; }
      };
      return Collections.singletonList(entry).iterator();
    }
  }

  @Override
  protected String getMavenUrl(MvnArtifact artifact) {
    return this.wmRuntimeInfo.getHttpBaseUrl() + "/" + artifact.getPath();
  }

  private void mockMvnMetadataResponses(WireMockRuntimeInfo wireMockRuntimeInfo) {
    Path ideHome = this.context.getIdeHome();
    if (ideHome == null) {
      return;
    }
    Path parent = ideHome.getParent();
    if (parent == null) {
      return;
    }
    Path mvnRoot = parent.resolve("repository").resolve("mvn");
    if (!Files.exists(mvnRoot)) {
      return;
    }
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
