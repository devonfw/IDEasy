package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * {@link JsonUrlUpdater} for Android Studio.
 */
public class AndroidStudioUrlUpdater extends JsonUrlUpdater<AndroidJsonObject, AndroidJsonItem> {

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
  protected void addVersion(UrlVersion urlVersion, AndroidJsonItem jsonVersionItem) {

    for (AndroidJsonDownload download : jsonVersionItem.getDownload()) {

      if (download.getLink().contains("windows.zip")) {
        doAddVersion(urlVersion, download.getLink(), WINDOWS, X64, download.getChecksum());
      } else if (download.getLink().contains("linux.tar.gz")) {
        doAddVersion(urlVersion, download.getLink(), LINUX, X64, download.getChecksum());
      } else if (download.getLink().contains("mac.zip")) {
        doAddVersion(urlVersion, download.getLink(), MAC, X64, download.getChecksum());
      } else if (download.getLink().contains("mac_arm.zip")) {
        doAddVersion(urlVersion, download.getLink(), MAC, ARM64, download.getChecksum());
      } else {
        logger.info("Unknown architecture for tool {} version {} and download {}.", getToolWithEdition(),
            jsonVersionItem.getVersion(), download.getLink());
      }
    }
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
  protected Collection<AndroidJsonItem> getVersionItems(AndroidJsonObject jsonObject) {

    return jsonObject.getContent().getItem();
  }

  @Override
  protected String getVersion(AndroidJsonItem jsonVersionItem) {

    return jsonVersionItem.getVersion();
  }

}
