package com.devonfw.tools.ide.url.tool.flutter;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractUrlUpdater} for the Flutter SDK (including the bundled Dart SDK).
 * <p>
 * Flutter does not publish a single version feed but one release feed per operating system ({@code releases_linux.json}, {@code releases_macos.json},
 * {@code releases_windows.json}) under the same base URL. This updater therefore fetches the three feeds individually and, unlike
 * {@link com.devonfw.tools.ide.url.updater.JsonUrlUpdater}, adds the download URLs directly from the feed entries. Only releases of the
 * {@value #CHANNEL_STABLE} channel are considered.
 */
public class FlutterUrlUpdater extends AbstractUrlUpdater {

  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  private static final Logger logger = LoggerFactory.getLogger(FlutterUrlUpdater.class);

  private static final String RELEASE_BASE_URL = "https://storage.googleapis.com/flutter_infra_release/releases";

  /** The name of the release feed for Linux (x64). */
  private static final String RELEASES_LINUX = "releases_linux.json";

  /** The name of the release feed for macOS (x64 and arm64). */
  private static final String RELEASES_MACOS = "releases_macos.json";

  /** The name of the release feed for Windows (x64). */
  private static final String RELEASES_WINDOWS = "releases_windows.json";

  /** The release channel that is the only one handled by this updater. */
  private static final String CHANNEL_STABLE = "stable";

  /**
   * The minimum Flutter version handled by this updater: {@code 3.0.0} is the first stable release that ships a macOS arm64 archive, and older releases
   * either lack the architectures we support or (in the case of the legacy {@code v1.x} releases) would sort <i>above</i> any modern version in
   * {@link VersionIdentifier} because of their leading {@code v} (a letter sorts above any digit). Skipping everything below {@code 3.0.0} therefore both
   * drops the ancient releases and stops a plain {@code ide install flutter} from resolving to a 2018 release via {@code resolveVersionPattern}.
   */
  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("3.0.0");

  /**
   * The constructor.
   */
  public FlutterUrlUpdater() {
    super(RELEASE_BASE_URL, RELEASE_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link FlutterUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  FlutterUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "flutter";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Fetches and processes the release feeds of all supported operating systems.
   */
  @Override
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    UrlEdition urlEdition = tool.getOrCreateChild(getEdition());
    setUrlUpdaterReport(new UrlUpdaterReport(tool.getName(), urlEdition.getName()));
    updateExistingVersions(urlEdition);
    processFeed(RELEASES_LINUX, LINUX, urlEdition);
    processFeed(RELEASES_MACOS, MAC, urlEdition);
    processFeed(RELEASES_WINDOWS, WINDOWS, urlEdition);
    getUrlFinalReport().addUrlUpdaterReport(getUrlUpdaterReport());
  }

  /**
   * Fetches the given release feed, filters for stable releases and adds the corresponding download URLs to the given {@link UrlEdition}.
   *
   * @param feedName the name of the release feed to fetch.
   * @param os the {@link OperatingSystem} this feed belongs to.
   * @param urlEdition the {@link UrlEdition} to add the versions and downloads to.
   */
  private void processFeed(String feedName, OperatingSystem os, UrlEdition urlEdition) {

    String feedUrl = getVersionBaseUrl() + "/" + feedName;
    String response = doGetResponseBodyAsString(feedUrl);
    FlutterJsonObject feed;
    try {
      feed = MAPPER.readValue(response, FlutterJsonObject.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Error while reading releases from " + feedUrl, e);
    }
    for (FlutterJsonItem item : feed.releases()) {
      if (isTimeoutExpired()) {
        break;
      }
      if (!CHANNEL_STABLE.equals(item.channel())) {
        continue;
      }
      try {
        String version = mapVersion(item.version());
        if (version == null) {
          continue;
        }
        UrlVersion urlVersion = urlEdition.getChild(version);
        if ((urlVersion == null) || isMissingOs(urlVersion)) {
          boolean newVersion = (urlVersion == null);
          urlVersion = urlEdition.getOrCreateChild(version);
          boolean added = doAddVersionFromFeed(urlVersion, feed, item, os);
          if (added) {
            if (newVersion) {
              getUrlUpdaterReport().incrementAddVersionSuccess();
            }
            logger.info("For tool {} we added version {}.", getToolWithEdition(), version);
          } else if (newVersion) {
            getUrlUpdaterReport().incrementAddVersionFailure();
          }
          urlVersion.save();
        }
      } catch (Exception e) {
        logger.error("For tool {} we failed to add version {}.", getToolWithEdition(), item.version(), e);
        getUrlUpdaterReport().incrementAddVersionFailure();
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * In addition to stripping the {@code v} prefix (see {@link #getVersionPrefixToRemove()}) we skip every stable release below {@link #MIN_VERSION}
   * {@code 3.0.0}. This is required because the legacy {@code v1.x} releases are <i>not</i> simply "old versions": their leading {@code v} is a letter, and
   * {@link VersionIdentifier} sorts a letter <i>above</i> any digit, so {@code v1.12.13} would otherwise resolve <i>above</i> {@code 3.47.5} and a plain
   * {@code ide install flutter} would pick a 2018 release.
   *
   * @param version the raw {@code version} field of a feed release (e.g. {@code v1.12.13+hotfix.9} or {@code 3.47.5}).
   * @return the normalized version to store (e.g. {@code 3.47.5}) or {@code null} if the release should be skipped (below {@link #MIN_VERSION}).
   */
  @Override
  public String mapVersion(String version) {

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
   * Legacy stable releases (all {@code v1.x}) carry a leading {@code v} that every release since {@code 2.0} lacks; it must be stripped so that the version
   * is stored and compared in a normalized form.
   *
   * @return {@code "v"}.
   */
  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  /**
   * Adds the download of the given release to the given {@link UrlVersion}. The download URL is composed from the {@code base_url} of the feed and the
   * relative {@code archive} of the release. The architecture is derived from the {@code dart_sdk_arch} of the release.
   *
   * @param urlVersion the {@link UrlVersion} to add the download to.
   * @param feed the {@link FlutterJsonObject} the release was read from (providing the {@code base_url}).
   * @param item the {@link FlutterJsonItem} of the release.
   * @param os the {@link OperatingSystem} of the release.
   * @return {@code true} if the download was added and verified, {@code false} otherwise.
   */
  private boolean doAddVersionFromFeed(UrlVersion urlVersion, FlutterJsonObject feed, FlutterJsonItem item, OperatingSystem os) {

    // A feed entry missing these would otherwise cause a cryptic NPE inside the base class; fail with a clear message instead.
    String baseUrl = feed.baseUrl();
    String archive = item.archive();
    if ((baseUrl == null) || (archive == null)) {
      logger.error("For tool {} the release {} has a missing base_url or archive in the feed.", getToolWithEdition(), item.version());
      return false;
    }
    String url = baseUrl + "/" + archive;
    return doAddVersion(urlVersion, url, os, getArchitecture(item), item.sha256());
  }

  /**
   * @param item the {@link FlutterJsonItem} of the release.
   * @return the {@link SystemArchitecture} derived from the {@code dart_sdk_arch} of the release.
   */
  private SystemArchitecture getArchitecture(FlutterJsonItem item) {

    if ("arm64".equalsIgnoreCase(item.dartSdkArch())) {
      return ARM64;
    }
    return X64;
  }

  @Override
  protected Set<String> getVersions() {

    throw new UnsupportedOperationException("The versions are collected from the release feeds in " + this.getClass().getSimpleName());
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    throw new UnsupportedOperationException("The versions are collected from the release feeds in " + this.getClass().getSimpleName());
  }

}
