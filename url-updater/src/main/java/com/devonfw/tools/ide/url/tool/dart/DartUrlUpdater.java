package com.devonfw.tools.ide.url.tool.dart;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractUrlUpdater} for the Dart SDK.
 * <p>
 * The official releases are published on Google Cloud Storage under the {@code dart-archive} bucket. Unlike
 * {@link com.devonfw.tools.ide.url.tool.flutter.FlutterUrlUpdater} (which reads a curated release feed per operating system), the bucket does not
 * expose a single feed of all past releases. This updater therefore enumerates the stable release folders via the GCS objects list API
 * ({@code /storage/v1/b/dart-archive/o?prefix=channels/stable/release/&delimiter=/}) and records the full stable history, so that a
 * {@code DART_VERSION} can be pinned. The version base URL therefore points at the host of the objects list API ({@code storage.googleapis.com}) while
 * the download base URL points at the {@code dart-archive} archive objects, which is why the two differ. As with Flutter, releases below
 * {@link #MIN_VERSION} are skipped: {@code 3.0.0} is the first stable
 * release that ships a macOS arm64 archive, and {@link #addVersion(UrlVersion)} unconditionally adds a {@code macos-arm64} download, so recording an
 * older release would produce a broken URL.
 */
public class DartUrlUpdater extends AbstractUrlUpdater {

  /** The base URL of the objects list API of the {@code dart-archive} bucket, used to enumerate the stable releases. */
  private static final String LIST_BASE_URL = "https://storage.googleapis.com";

  /** The relative path of the objects list API that enumerates the stable release folders of the {@code dart-archive} bucket. */
  private static final String LIST_API = "/storage/v1/b/dart-archive/o?prefix=channels/stable/release/&delimiter=/";

  /** The base URL of the official Dart release archives on Google Cloud Storage. */
  private static final String RELEASE_BASE_URL = "https://storage.googleapis.com/dart-archive";

  /** The prefix that the objects list API returns for every stable release folder (including the {@code latest} alias), to be stripped. */
  private static final String RELEASE_PREFIX = "channels/stable/release/";

  /** The trailing {@code /} that the objects list API appends to every listed prefix. */
  private static final String RELEASE_PREFIX_SUFFIX = "/";

  /** The path template to a Dart SDK archive, relative to {@link #RELEASE_BASE_URL}. */
  private static final String SDK_ARCHIVE = "/channels/stable/release/${version}/sdk/dartsdk-";

  /**
   * The minimum Dart version handled by this updater: {@code 3.0.0} is the first stable release that ships a macOS arm64 archive. This mirrors
   * {@link com.devonfw.tools.ide.url.tool.flutter.FlutterUrlUpdater}: since {@link #addVersion(UrlVersion)} unconditionally adds a
   * {@code macos-arm64} download, recording an older release would produce a broken URL for that platform.
   */
  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("3.0.0");

  /** The {@link Logger}. */
  private static final Logger logger = LoggerFactory.getLogger(DartUrlUpdater.class);

  /** The shared {@link ObjectMapper}. */
  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  /**
   * The constructor.
   */
  public DartUrlUpdater() {
    super(RELEASE_BASE_URL, LIST_BASE_URL);
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
   * Returns the full stable version history, enumerated from the objects list of the {@code dart-archive} bucket. The {@code latest} alias folder and
   * every release below {@link #MIN_VERSION} are filtered out (see {@link #mapVersion(String)}).
   */
  @Override
  protected Set<String> getVersions() {
    String listUrl = getVersionBaseUrl() + LIST_API;
    String response = doGetResponseBodyAsString(listUrl);
    DartReleaseList releaseList;
    try {
      releaseList = MAPPER.readValue(response, DartReleaseList.class);
    } catch (IOException e) {
      throw new IllegalStateException("Error while reading releases from " + listUrl, e);
    }
    Set<String> versions = new HashSet<>();
    for (String prefix : releaseList.prefixes()) {
      if (isTimeoutExpired()) {
        break;
      }
      if (!prefix.startsWith(RELEASE_PREFIX) || !prefix.endsWith(RELEASE_PREFIX_SUFFIX)) {
        continue;
      }
      addVersion(prefix.substring(RELEASE_PREFIX.length(), prefix.length() - RELEASE_PREFIX_SUFFIX.length()), versions);
    }
    return versions;
  }

  /**
   * {@inheritDoc}
   * <p>
   * In addition to the base class filtering, rejects the {@code latest} alias (which is not a concrete release) and every release below
   * {@link #MIN_VERSION}.
   *
   * @param version the stable release folder name to check (e.g. {@code 3.13.5} or {@code latest}).
   * @return the version to store, or {@code null} to skip it.
   */
  @Override
  public String mapVersion(String version) {

    if (VersionIdentifier.LATEST.equals(VersionIdentifier.of(version))) {
      return null;
    }
    String normalized = super.mapVersion(version);
    if (normalized == null) {
      return null;
    }
    VersionIdentifier versionIdentifier = VersionIdentifier.of(normalized);
    return ((versionIdentifier == null) || versionIdentifier.compareVersion(MIN_VERSION).isLess()) ? null : normalized;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Adds the download URL for each supported operating system and architecture. As Dart does not publish checksums, the checksum is computed on
   * download.
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
