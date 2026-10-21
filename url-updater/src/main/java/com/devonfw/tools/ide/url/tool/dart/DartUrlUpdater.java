package com.devonfw.tools.ide.url.tool.dart;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractUrlUpdater} for the Dart SDK.
 * <p>
 * The official releases are published on Google Cloud Storage under the {@code dart-archive} bucket. The {@code VERSION} file of the {@code stable} channel
 * points to the latest release, which this updater tracks. Newer releases accumulate in the URL repository over time as this updater is run, as existing
 * versions are never re-downloaded nor removed.
 */
public class DartUrlUpdater extends AbstractUrlUpdater {

  /** The base URL of the official Dart releases on Google Cloud Storage. */
  private static final String RELEASE_BASE_URL = "https://storage.googleapis.com/dart-archive";

  /** The release channel that is the only one handled by this updater. */
  private static final String CHANNEL_STABLE = "stable";

  /** The path to the {@code VERSION} file of the stable channel. */
  private static final String VERSION_FEED = "/channels/stable/release/latest/VERSION";

  /** The path template to a Dart SDK archive, relative to {@link #RELEASE_BASE_URL}. */
  private static final String SDK_ARCHIVE = "/channels/stable/release/${version}/sdk/dartsdk-";

  /** The {@link Logger}. */
  private static final Logger logger = LoggerFactory.getLogger(DartUrlUpdater.class);

  /** The shared {@link ObjectMapper}. */
  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  /**
   * The constructor.
   */
  public DartUrlUpdater() {
    super(RELEASE_BASE_URL, RELEASE_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link DartUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  DartUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {
    return "dart";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the latest version of the stable channel, read from the {@code VERSION} feed.
   */
  @Override
  protected Set<String> getVersions() {
    String feedUrl = getVersionBaseUrl() + VERSION_FEED;
    String response = doGetResponseBodyAsString(feedUrl);
    try {
      DartJsonItem feed = MAPPER.readValue(response, DartJsonItem.class);
      String version = feed.version();
      if ((version != null) && !version.isEmpty()) {
        return Set.of(version);
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Error while reading version from " + feedUrl, e);
    }
    logger.warn("For tool {} the latest version could not be determined from {}.", getTool(), feedUrl);
    return Set.of();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Adds the download URL for each supported operating system and architecture. As Dart does not publish checksums, the checksum is computed on download.
   */
  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = getDownloadBaseUrl() + SDK_ARCHIVE;
    doAddVersion(urlVersion, baseUrl + "linux-x64-release.zip", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-arm64-release.zip", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "macos-x64-release.zip", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "macos-arm64-release.zip", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "windows-x64-release.zip", WINDOWS, X64);
  }
}
