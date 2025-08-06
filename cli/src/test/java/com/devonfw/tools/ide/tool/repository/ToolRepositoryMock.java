package com.devonfw.tools.ide.tool.repository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Implementation of {@link ToolRepository} for testing.
 */
public class ToolRepositoryMock extends DefaultToolRepository {

  private static final String VERSION_DEFAULT = "default";
  private static final String VARIABLE_TESTBASEURL = "${testbaseurl}";

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
        String extension = FilenameUtil.getExtension(url);
        byte[] body = createReleaseDownload(archiveFolder, extension);
        String path = url.substring(VARIABLE_TESTBASEURL.length());
        stubFor(get(urlMatching(path)).willReturn(
            aResponse().withStatus(200).withBody(body)));
        String resolvedUrl = url.replace(VARIABLE_TESTBASEURL, this.wmRuntimeInfo.getHttpBaseUrl());
        UrlDownloadFile urlDownloadFile = (UrlDownloadFile) metadata;
        metadata = new UrlDownloadFile(urlDownloadFile.getParent(), urlDownloadFile.getName());
        Set<String> urls = metadata.getUrls();
        urls.clear(); // this is a hack since the file was just loaded again from disc
        urls.add(resolvedUrl);
        return super.download(metadata);
      } else {
        throw new IllegalStateException("Invalid URL: " + url);
      }
    }
    return archiveFolder;
  }

  private byte[] createReleaseDownload(Path releaseContentFolder, String extension) {

    try {
      ByteArrayOutputStream out;
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
        out = baos;
        if (extension.equals("zip")) {
          try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(baos)) {
            writeReleaseDownload(releaseContentFolder, zipOut, "");
            zipOut.finish();
          }
        } else if (extension.equals("tgz")) {
          try (GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(baos);
              TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)) {
            writeReleaseDownload(releaseContentFolder, tarOut, "");
            tarOut.finish();
          }
        } else {
          throw new IllegalArgumentException("Unsupported extension: " + extension);
        }
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create release download for " + releaseContentFolder, e);
    }
  }

  private <E extends ArchiveEntry> void writeReleaseDownload(Path path, ArchiveOutputStream<E> out, String relativePath) {
    try (Stream<Path> childStream = Files.list(path)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        String relativeChildPath = relativePath + "/" + child.getFileName().toString();
        boolean isDirectory = Files.isDirectory(child);
        E archiveEntry = out.createArchiveEntry(child, relativeChildPath);
        if (archiveEntry instanceof TarArchiveEntry tarEntry) {
          FileTime none = FileTime.fromMillis(0);
          tarEntry.setCreationTime(none);
          tarEntry.setModTime(none);
          tarEntry.setLastAccessTime(none);
          tarEntry.setUserName("user");
          tarEntry.setGroupName("group");
          if (relativePath.endsWith("bin") && !isDirectory) {
            tarEntry.setMode(tarEntry.getMode() | 0111);
          }
        }
        out.putArchiveEntry(archiveEntry);
        if (!isDirectory) {
          try (InputStream in = Files.newInputStream(child)) {
            IOUtils.copy(in, out);
          }
        }
        out.closeArchiveEntry();
        if (isDirectory) {
          writeReleaseDownload(child, out, relativeChildPath);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to compress " + path, e);
    }
  }
}
