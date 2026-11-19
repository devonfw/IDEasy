package com.devonfw.tools.ide.url.tool.dart;

import java.util.Collection;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link JsonUrlUpdater} for the Dart SDK.
 * <p>
 * The official releases are published on Google Cloud Storage under the {@code dart-archive} bucket, which does not expose a curated per-release feed (unlike
 * {@link com.devonfw.tools.ide.url.tool.flutter.FlutterUrlUpdater}). It therefore enumerates the stable release folders via the single JSON listing returned by
 * the GCS objects list API ({@code /storage/v1/b/dart-archive/o?prefix=channels/stable/release/&delimiter=/}) and maps every folder prefix to a version item.
 * The download URLs are then composed from a template per operating system and architecture, so that a
 * {@code DART_VERSION} can be pinned to any stable release. As with Flutter, releases below {@link #MIN_VERSION}
 * are skipped: {@code 3.0.0} is the first stable release that ships a macOS arm64 archive, and
 * {@link #addVersion(UrlVersion, DartRelease)} unconditionally adds a {@code macos-arm64} download, so recording
 * an older release would produce a broken URL.
 */
public class DartUrlUpdater extends JsonUrlUpdater<DartReleaseList, DartRelease> {

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

  /** The first stable release that publishes a Windows arm64 archive. */
  private static final VersionIdentifier WINDOWS_ARM64_MIN_VERSION = VersionIdentifier.ofRequired("3.3.0");

  /**
   * The minimum Dart version handled by this updater: {@code 3.0.0} is the first stable release that ships a macOS arm64 archive. This mirrors
   * {@link com.devonfw.tools.ide.url.tool.flutter.FlutterUrlUpdater}: since {@link #addVersion(UrlVersion, DartRelease)} unconditionally adds a
   * {@code macos-arm64} download, recording an older release would produce a broken URL for that platform.
   */
  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.ofRequired("3.0.0");

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
   * Points at the GCS objects list API of the {@code dart-archive} bucket, which lists the stable release folders as JSON.
   */
  @Override
  protected String doGetVersionUrl() {
    return getVersionBaseUrl() + LIST_API;
  }

  @Override
  protected Class<DartReleaseList> getJsonObjectType() {
    return DartReleaseList.class;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Maps the {@code prefixes} of the objects list API to the release folder names (e.g. {@code 3.13.5}). Folders that do not match the
   * {@link #RELEASE_PREFIX} (and therefore are not release folders) are ignored.
   *
   * @param jsonObject the parsed objects list response.
   * @return the collection of release version items.
   */
  @Override
  protected Collection<DartRelease> getVersionItems(DartReleaseList jsonObject) {
    return jsonObject.prefixes().stream()
        .filter(prefix -> prefix.startsWith(RELEASE_PREFIX) && prefix.endsWith(RELEASE_PREFIX_SUFFIX))
        .map(prefix -> new DartRelease(prefix.substring(RELEASE_PREFIX.length(), prefix.length() - RELEASE_PREFIX_SUFFIX.length())))
        .toList();
  }

  /**
   * {@inheritDoc}
   * <p>
   * In addition to the base class filtering, rejects every release folder that is not a dotted {@code x.y.z} version (which also covers the {@code latest}
   * alias, being a non-concrete release) and every release below {@link #MIN_VERSION}.
   *
   * @param version the stable release folder name to check (e.g. {@code 3.13.5} or {@code latest}).
   * @return the version to store, or {@code null} to skip it.
   */
  @Override
  public String mapVersion(String version) {

    if (!version.matches("\\d+\\.\\d+\\.\\d+")) {
      return null;
    }
    String normalized = super.mapVersion(version);
    if (normalized == null) {
      return null;
    }
    VersionIdentifier versionIdentifier = VersionIdentifier.ofOptional(normalized);
    return ((versionIdentifier == null) || versionIdentifier.compareVersion(MIN_VERSION).isLess()) ? null : normalized;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Adds the download URL for each supported operating system and architecture. As Dart does not publish checksums, the checksum is computed on download.
   */
  @Override
  protected void addVersion(UrlVersion urlVersion, DartRelease release) {
    String baseUrl = getDownloadBaseUrl() + SDK_ARCHIVE;
    doAddVersion(urlVersion, baseUrl + "linux-x64-release.zip", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "linux-arm64-release.zip", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "macos-x64-release.zip", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "macos-arm64-release.zip", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "windows-x64-release.zip", WINDOWS, X64);
    // Dart does not publish a Windows arm64 archive before 3.3.0 (introduced with Dart 3.3.0), so only add it for newer releases.
    if (!urlVersion.getVersionIdentifier().compareVersion(WINDOWS_ARM64_MIN_VERSION).isLess()) {
      doAddVersion(urlVersion, baseUrl + "windows-arm64-release.zip", WINDOWS, ARM64);
    }
  }
}
