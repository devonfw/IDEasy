package com.devonfw.tools.ide.url.tool.androidstudio;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;

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

    for (AndroidJsonDownload download : jsonVersionItem.download()) {

      if (download.link().contains("windows.zip")) {
        doAddVersion(urlVersion, download.link(), WINDOWS, X64, download.checksum());
      } else if (download.link().contains("linux.tar.gz")) {
        doAddVersion(urlVersion, download.link(), LINUX, X64, download.checksum());
      } else if (download.link().contains("mac.zip")) {
        doAddVersion(urlVersion, download.link(), MAC, X64, download.checksum());
      } else if (download.link().contains("mac_arm.zip")) {
        doAddVersion(urlVersion, download.link(), MAC, ARM64, download.checksum());
      } else {
        logger.info("Unknown architecture for tool {} version {} and download {}.", getToolWithEdition(),
            jsonVersionItem.version(), download.link());
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

    return jsonObject.content().item();
  }

  @Override
  public String getCpeVendor() {
    return "google";
  }

  @Override
  public String getCpeProduct() {
    return "android studio";
  }
}
