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
    try {
      FlutterJsonObject feed = MAPPER.readValue(response, FlutterJsonObject.class);
      for (FlutterJsonItem item : feed.releases()) {
        if (!CHANNEL_STABLE.equals(item.channel()) || isTimeoutExpired()) {
          continue;
        }
        String version = item.version();
        UrlVersion urlVersion = urlEdition.getChild(version);
        if (urlVersion == null || isMissingOs(urlVersion)) {
          boolean newVersion = (urlVersion == null);
          urlVersion = urlEdition.getOrCreateChild(version);
          boolean added = doAddVersionFromFeed(urlVersion, feed, item, os);
          if (newVersion && added) {
            getUrlUpdaterReport().incrementAddVersionSuccess();
          }
          urlVersion.save();
          logger.info("For tool {} we added version {}.", getToolWithEdition(), version);
        }
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Error while reading releases from " + feedUrl, e);
    }
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

    String url = feed.baseUrl() + "/" + item.archive();
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
