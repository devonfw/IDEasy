package com.devonfw.tools.ide.url.updater.androidstudio;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * {@link JsonUrlUpdater} for Android Studio.
 */
public class AndroidStudioUrlUpdater extends JsonUrlUpdater<AndroidJsonObject> {

  /** The base URL for the version json file */
  private final static String VERSION_BASE_URL = "https://jb.gg";

  /** The name of the version json file */
  private final static String VERSION_FILENAME = "android-studio-releases-list.json";

  private static final Logger logger = LoggerFactory.getLogger(AndroidStudioUrlUpdater.class);

  /**
   * @return String of version base URL
   */
  protected String getVersionBaseUrl() {

    return VERSION_BASE_URL;
  }

  @Override
  protected String getTool() {

    return "android-studio";
  }

  @Override
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    UrlEdition edition = tool.getOrCreateChild(getEdition());
    updateExistingVersions(edition);
    String toolWithEdition = getToolWithEdition();
    ObjectMapper MAPPER = JsonMapping.create();
    String url = doGetVersionUrl();
    try {
      String response = doGetResponseBodyAsString(url);
      AndroidJsonObject jsonObject = MAPPER.readValue(response, getJsonObjectType());

      List<AndroidJsonItem> items = jsonObject.getContent().getItem();

      for (AndroidJsonItem item : items) {
        String version = item.getVersion();

        if (isTimeoutExpired()) {
          break;
        }

        UrlVersion urlVersion = edition.getChild(version);
        if (urlVersion == null || isMissingOs(urlVersion)) {
          try {
            urlVersion = edition.getOrCreateChild(version);
            for (AndroidJsonDownload download : item.getDownload()) {

              if (download.getLink().contains("windows.zip")) {
                doAddVersion(urlVersion, download.getLink(), WINDOWS, X64, download.getChecksum());
              } else if (download.getLink().contains("linux.tar.gz")) {
                doAddVersion(urlVersion, download.getLink(), LINUX, X64, download.getChecksum());
              } else if (download.getLink().contains("mac.zip")) {
                doAddVersion(urlVersion, download.getLink(), MAC, X64, download.getChecksum());
              } else if (download.getLink().contains("mac_arm.zip")) {
                doAddVersion(urlVersion, download.getLink(), MAC, ARM64, download.getChecksum());
              } else {
                logger.info("Unknown architecture for tool {} version {} and download {}.", toolWithEdition, version,
                    download.getLink());
              }
            }
            urlVersion.save();
          } catch (Exception e) {
            logger.error("For tool {} we failed to add version {}.", toolWithEdition, version, e);
          }

        }
      }
    } catch (Exception e2) {
      throw new IllegalStateException("Error while getting versions from JSON API " + url, e2);
    }

  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    throw new IllegalStateException();
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + VERSION_FILENAME;
  }

  @Override
  protected Class<AndroidJsonObject> getJsonObjectType() {

    return AndroidJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(AndroidJsonObject jsonItem, Collection<String> versions) {

    throw new IllegalStateException();
  }

}
