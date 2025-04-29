package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonDownloadsItem;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonObject;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonRelease;

/**
 * {@link JsonUrlUpdater} for packages from the intelliJ IDEA registry.
 */
public abstract class IdeaBasedUrlUpdater extends JsonUrlUpdater<IntellijJsonObject, IntellijJsonRelease> {

  protected static final String VERSION_BASE_URL = "https://data.services.jetbrains.com";

  /**
   * @return String of version base URL
   */
  protected String getVersionBaseUrl() {

    return VERSION_BASE_URL;
  }

  @Override
  protected Collection<IntellijJsonRelease> getVersionItems(IntellijJsonObject jsonObject) {

    return jsonObject.releases();
  }


  @Override
  protected Class<IntellijJsonObject> getJsonObjectType() {

    return IntellijJsonObject.class;
  }

  /**
   * Follows link and gets body as string which contains checksum
   */
  private String getCheckSum(String checksumLink) {

    String responseCS = doGetResponseBodyAsString(checksumLink);
    return responseCS.split(" ")[0];
  }

  /**
   * Get link and link for the checksum for each OS, which are separate nodes in the json
   *
   * @param url {@link UrlVersion} to be updated
   * @param release the {@link JsonVersionItem} holding the download references for the version
   * @param jsonOS the OS as specified in the json
   * @param os the {@link OperatingSystem} matching the jsonOS
   * @param systemArchitecture {@link SystemArchitecture} of the version to be updated
   */
  private void addVersionEachOs(UrlVersion url, IntellijJsonRelease release, String jsonOS, OperatingSystem os,
      SystemArchitecture systemArchitecture) {

    IntellijJsonDownloadsItem downloadItem = release.downloads().get(jsonOS);
    String link = downloadItem.getLink();
    String checkSumLink = downloadItem.getChecksumLink();
    if (checkSumLink.isEmpty()) {
      doAddVersion(url, link, os, systemArchitecture);
    } else {
      String cs = getCheckSum(checkSumLink);
      doAddVersion(url, link, os, systemArchitecture, cs);
    }

  }

  @Override
  protected void addVersion(UrlVersion urlVersion, IntellijJsonRelease release) {

    for (String os : release.downloads().keySet()) {
      switch (os) {
        case IntellijJsonRelease.KEY_WINDOWS:
          addVersionEachOs(urlVersion, release, os, OperatingSystem.WINDOWS, SystemArchitecture.X64);
          break;
        case IntellijJsonRelease.KEY_LINUX:
          addVersionEachOs(urlVersion, release, os, OperatingSystem.LINUX, SystemArchitecture.X64);
          break;
        case IntellijJsonRelease.KEY_MAC:
          addVersionEachOs(urlVersion, release, os, OperatingSystem.MAC, SystemArchitecture.X64);
          break;
        case IntellijJsonRelease.KEY_MAC_ARM:
          addVersionEachOs(urlVersion, release, os, OperatingSystem.MAC, SystemArchitecture.ARM64);
          break;
      }
    }
  }


}
