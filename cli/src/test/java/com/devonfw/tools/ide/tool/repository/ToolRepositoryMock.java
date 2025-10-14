package com.devonfw.tools.ide.tool.repository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Implementation of {@link ToolRepository} for testing.
 */
public class ToolRepositoryMock extends DefaultToolRepository {

  private static final String VERSION_DEFAULT = "default";

  /** Variable to be used as base url for WireMock url replacement */
  public static final String VARIABLE_TESTBASEURL = "${testbaseurl}";

  private final Path repositoryFolder;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param repositoryFolder the {@link Path} to the mock repository.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} for mocking HTTP requests or {@code null} to use copy instead of download.
   */
  public ToolRepositoryMock(IdeContext context, Path repositoryFolder, WireMockRuntimeInfo wmRuntimeInfo) {

    super(context);
    this.repositoryFolder = repositoryFolder;
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    return super.resolveVersion(tool, edition, version, toolCommandlet);

  }

  @Override
  public Path download(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    Path editionFolder = this.repositoryFolder.resolve(tool).resolve(edition);
    String versionString = version.toString();
    Path versionFolder = editionFolder.resolve(versionString);
    if (!Files.isDirectory(versionFolder)) {
      this.context.debug("Could not find version {} so using 'default' for {}/{}", version, tool, edition);
      versionString = VERSION_DEFAULT;
      versionFolder = editionFolder.resolve(versionString);
    }
    if (!Files.isDirectory(versionFolder)) {
      throw new IllegalStateException("Mock download failed - could not find folder " + editionFolder);
    }
    Path archiveFolder = versionFolder.resolve(this.context.getSystemInfo().getOs().toString());
    if (!Files.isDirectory(archiveFolder)) {
      archiveFolder = versionFolder;
    }
    Path contentArchive = null;
    try (Stream<Path> children = Files.list(archiveFolder)) {
      Iterator<Path> iterator = children.iterator();
      while (iterator.hasNext()) {
        if (contentArchive == null) {
          Path child = iterator.next();
          if (Files.isRegularFile(child) && child.getFileName().startsWith("content.")) {
            contentArchive = child;
            this.context.debug("Using compressed archive {} for mock download of {}/{}", child.getFileName(), tool,
                edition);
          } else {
            break;
          }
        } else {
          contentArchive = null;
          break;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list children of folder " + archiveFolder);
    }
    if (contentArchive != null) {
      return contentArchive;
    }
    if (this.wmRuntimeInfo != null) {
      UrlDownloadFileMetadata metadata = getMetadata(tool, edition, version, toolCommandlet);
      String url = metadata.getUrls().iterator().next();
      if (url.startsWith(VARIABLE_TESTBASEURL)) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
          this.context.getFileAccess().compress(archiveFolder, baos, url);
          byte[] body = baos.toByteArray();
          String path = url.substring(VARIABLE_TESTBASEURL.length());
          stubFor(get(urlMatching(path)).willReturn(
              aResponse().withStatus(200).withBody(body)));
          String resolvedUrl = url.replace(VARIABLE_TESTBASEURL, this.wmRuntimeInfo.getHttpBaseUrl());
          UrlDownloadFile urlDownloadFile = (UrlDownloadFile) metadata;
          metadata = new UrlDownloadFile(urlDownloadFile.getParent(), urlDownloadFile.getName(), Set.of(resolvedUrl));
          return super.download(metadata);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to create mock archive for " + url, e);
        }
      } else {
        throw new IllegalStateException("Invalid URL: " + url);
      }
    }
    return archiveFolder;
  }

}
