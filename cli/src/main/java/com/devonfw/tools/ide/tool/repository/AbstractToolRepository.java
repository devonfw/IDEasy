package com.devonfw.tools.ide.tool.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract base implementation of {@link ToolRepository}.
 */
public abstract class AbstractToolRepository implements ToolRepository {

  private static final int MAX_TEMP_DOWNLOADS = 9;

  /** The owning {@link IdeContext}. */
  protected final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public AbstractToolRepository(IdeContext context) {

    super();
    this.context = context;
  }

  /**
   * @param tool the name of the tool to download.
   * @param edition the edition of the tool to download.
   * @param version the {@link VersionIdentifier} to download.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the resolved {@link UrlDownloadFileMetadata}.
   */
  protected abstract UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet);


  @Override
  public Path download(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    UrlDownloadFileMetadata metadata = getMetadata(tool, edition, version, toolCommandlet);
    return download(metadata);
  }

  /**
   * @param metadata the {@link UrlDownloadFileMetadata}.
   * @return the {@link Path} to the downloaded file.
   */
  public Path download(UrlDownloadFileMetadata metadata) {

    VersionIdentifier version = metadata.getVersion();
    if (context.isOffline()) {
      throw CliOfflineException.ofDownloadOfTool(metadata.getTool(), metadata.getEdition(), version);
    }
    Set<String> urlCollection = metadata.getUrls();
    if (urlCollection.isEmpty()) {
      throw new IllegalStateException("Invalid download metadata with empty urls file for " + metadata);
    }
    Path target = doDownload(metadata);
    return target;
  }

  /**
   * @param metadata the {@link UrlDownloadFileMetadata}.
   * @return the {@link Path} to the downloaded file.
   */
  protected Path doDownload(UrlDownloadFileMetadata metadata) {
    String downloadFilename = createDownloadFilename(metadata.getTool(), metadata.getEdition(), metadata.getVersion(), metadata.getOs(),
        metadata.getArch(), metadata.getUrls().iterator().next());
    Path downloadCache = this.context.getDownloadPath().resolve(getId());
    this.context.getFileAccess().mkdirs(downloadCache);
    Path target = downloadCache.resolve(downloadFilename);
    if (Files.exists(target)) {
      this.context.interaction("Artifact already exists at {}\nTo force update please delete the file and run again.", target);
    } else {
      target = download(metadata, target);
    }
    return target;
  }

  /**
   * @param metadata the {@link UrlDownloadFileMetadata} for the download.
   * @param target the expected {@link Path} to download to.
   * @return the actual {@link Path} of the downloaded file.
   */
  private Path download(UrlDownloadFileMetadata metadata, Path target) {

    VersionIdentifier resolvedVersion = metadata.getVersion();
    List<String> urlList = new ArrayList<>(metadata.getUrls());
    if (urlList.size() > 1) {
      Collections.shuffle(urlList);
    }
    UrlChecksums checksums = metadata.getChecksums();
    for (String url : urlList) {
      try {
        return download(url, target, resolvedVersion, checksums);
      } catch (Exception e) {
        this.context.error(e, "Failed to download from " + url);
      }
    }
    throw new CliException("Download of " + target.getFileName() + " failed after trying " + urlList.size() + " URL(s).");
  }

  /**
   * Computes the normalized filename of the download package. It uses the schema {@code «tool»-«version»[-«edition»][-«os»][-«arch»].«ext»}.
   *
   * @param tool the name of the tool to download.
   * @param edition the edition of the tool to download.
   * @param version the resolved {@link VersionIdentifier} of the tool to download.
   * @param os the specific {@link OperatingSystem} or {@code null} if the download is OS-agnostic.
   * @param arc the specific {@link SystemArchitecture} or {@code null} if the download is arc-agnostic.
   * @param url the download URL used to determine the file-extension ({@code «ext»}).
   * @return the computed filename.
   */
  protected String createDownloadFilename(String tool, String edition, VersionIdentifier version, OperatingSystem os,
      SystemArchitecture arc, String url) {

    StringBuilder sb = new StringBuilder(32);
    sb.append(tool);
    sb.append("-");
    if (VersionIdentifier.LATEST.equals(version)) {
      sb.append("latest");
    } else {
      sb.append(version);
    }
    if (!edition.equals(tool)) {
      sb.append("-");
      sb.append(edition);
    }
    if (os != null) {
      sb.append("-");
      sb.append(os);
    }
    if (arc != null) {
      sb.append("-");
      sb.append(arc);
    }
    String extension = FilenameUtil.getExtension(url);
    if (extension == null) {
      // legacy fallback - should never happen
      if (this.context.getSystemInfo().isLinux()) {
        extension = "tgz";
      } else {
        extension = "zip";
      }
      this.context.warning("Could not determine file extension from URL {} - guess was {} but may be incorrect.", url,
          extension);
    }
    sb.append(".");
    sb.append(extension);
    return sb.toString();
  }

  /**
   * @param url the URL to download from.
   * @param target the {@link Path} to the target file to download to.
   * @param resolvedVersion the resolved version to download as {@link VersionIdentifier} or {@link String}.
   * @param expectedChecksums the {@link UrlChecksums}.
   * @return the actual {@link Path} where the file was downloaded to. Typically the given {@link Path} {@code target} but may also be a different file in
   *     edge-cases.
   */
  protected Path download(String url, Path target, Object resolvedVersion, UrlChecksums expectedChecksums) {

    String downloadFilename = target.getFileName().toString();
    Path tmpDownloadFile = createTempDownload(downloadFilename);
    Path result;
    try {
      this.context.getFileAccess().download(url, tmpDownloadFile);
      verifyChecksums(tmpDownloadFile, expectedChecksums, resolvedVersion);
      if (isLatestVersion(resolvedVersion)) {
        // Some software vendors violate best-practices and provide the latest version only under a fixed URL.
        // Therefore, if a newer version of that file gets released, the same URL suddenly leads to a different
        // download file with a newer version and a different checksum.
        // In order to still support such tools we had to implement this workaround so we cannot move the file in the
        // download cache for later reuse, cannot verify its checksum and also delete the downloaded file on exit
        // (after we assume it has been extracted) so we always ensure to get the LATEST version when requested.
        tmpDownloadFile.toFile().deleteOnExit();
        result = tmpDownloadFile;
      } else {
        this.context.getFileAccess().move(tmpDownloadFile, target);
        result = target;
      }
    } catch (RuntimeException e) {
      this.context.getFileAccess().delete(tmpDownloadFile);
      throw e;
    }
    return result;
  }

  private static boolean isLatestVersion(Object resolvedVersion) {
    return resolvedVersion.toString().equals("latest");
  }

  /**
   * @param filename the name of the temporary download file.
   * @return a {@link Path} to such file that does not yet exist.
   */
  protected Path createTempDownload(String filename) {

    Path tmpDownloads = this.context.getTempDownloadPath();
    Path tmpDownloadFile = tmpDownloads.resolve(filename);
    int i = 2;
    while (Files.exists(tmpDownloadFile)) {
      tmpDownloadFile = tmpDownloads.resolve(filename + "." + i);
      i++;
      if (i > MAX_TEMP_DOWNLOADS) {
        throw new IllegalStateException("Too many downloads of the same file: " + tmpDownloadFile);
      }
    }
    return tmpDownloadFile;
  }

  private void verifyChecksums(Path file, UrlChecksums expectedChecksums, Object version) {

    if (expectedChecksums == null) {
      return;
    }
    boolean checksumVerified = false;
    for (UrlGenericChecksum expectedChecksum : expectedChecksums) {
      verifyChecksum(file, expectedChecksum);
      checksumVerified = true;
    }
    if (!checksumVerified) {
      IdeLogLevel level = IdeLogLevel.WARNING;
      if (isLatestVersion(version)) {
        level = IdeLogLevel.DEBUG;
      }
      this.context.level(level).log("No checksum found for {}", file);
    }
  }

  /**
   * Performs the checksum verification.
   *
   * @param file the downloaded software package to verify.
   * @param expectedChecksum the expected SHA-256 checksum.
   */
  protected void verifyChecksum(Path file, UrlGenericChecksum expectedChecksum) {

    String hashAlgorithm = expectedChecksum.getHashAlgorithm();
    String actualChecksum = this.context.getFileAccess().checksum(file, hashAlgorithm);
    if (expectedChecksum.getChecksum().equals(actualChecksum)) {
      this.context.success("{} checksum {} is correct.", hashAlgorithm, actualChecksum);
    } else {
      throw new CliException("Downloaded file " + file + " has the wrong " + hashAlgorithm + " checksum!\n" //
          + "Expected " + expectedChecksum + "\n" //
          + "Download " + actualChecksum + "\n" //
          + "This could be a man-in-the-middle-attack, a download failure, or a release that has been updated afterwards.\n" //
          + "Please review carefully.\n" //
          + "Expected checksum can be found at " + expectedChecksum + ".\n" //
          + "Installation was aborted for security reasons!");
    }
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    List<VersionIdentifier> versions = getSortedVersions(tool, edition, toolCommandlet);
    return VersionIdentifier.resolveVersionPattern(version, versions, this.context);
  }
}
