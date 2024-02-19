package com.devonfw.tools.ide.tool.intellij;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link IntellijUrlUpdater} base class for IntelliJ.
 */
public class IntellijUrlUpdater extends JsonUrlUpdater<IntellijJsonObject> {

  private static final String VERSION_BASE_URL = "https://data.services.jetbrains.com";

  private static final String JSON_URL = "products?code=IIU%2CIIC&release.type=release";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final Logger logger = LoggerFactory.getLogger(IntellijUrlUpdater.class);

  @Override
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    try {
      String response = doGetResponseBodyAsString(doGetVersionUrl());
      IntellijJsonObject[] jsonObj = MAPPER.readValue(response, IntellijJsonObject[].class);

      if (jsonObj.length == 2) {
        IntellijJsonObject release = getIntellijJsonRelease(jsonObj);
        UrlEdition edition = tool.getOrCreateChild(getEdition());

        if (release != null) {
          addVersionForEdition(release, edition);
        }
      }

    } catch (Exception e) {
      throw new IllegalStateException("Error while getting versions from JSON API " + JSON_URL, e);
    }
  }

  /**
   * @param releases Has 2 elements, 1. Ultimate Edition, 2. Community Edition
   * @return The release for the {@link #getEdition() edition}.
   */
  public IntellijJsonObject getIntellijJsonRelease(IntellijJsonObject[] releases) {

    return releases[1];
  }

  /**
   * Adds a version for the provided {@link UrlEdition}
   *
   * @param release the {@link IntellijJsonObject}
   * @param edition the {@link UrlEdition}
   */
  private void addVersionForEdition(IntellijJsonObject release, UrlEdition edition) {

    updateExistingVersions(edition);
    String toolWithEdition = getToolWithEdition();

    List<IntellijJsonRelease> releases = release.getReleases();
    for (IntellijJsonRelease r : releases) {
      String version = r.getVersion();
      Map<String, IntellijJsonDownloadsItem> downloads = r.getDownloads();
      UrlVersion urlVersion = edition.getChild(version);

      if (urlVersion == null || isMissingOs(urlVersion)) {
        try {
          urlVersion = edition.getOrCreateChild(version);
          for (String os : downloads.keySet()) {
            switch (os) {
              case IntellijJsonRelease.KEY_WINDOWS:
                addVersionEachOs(urlVersion, downloads, os, OperatingSystem.WINDOWS, SystemArchitecture.X64);
                break;
              case IntellijJsonRelease.KEY_LINUX:
                addVersionEachOs(urlVersion, downloads, os, OperatingSystem.LINUX, SystemArchitecture.X64);
                break;
              case IntellijJsonRelease.KEY_MAC:
                addVersionEachOs(urlVersion, downloads, os, OperatingSystem.MAC, SystemArchitecture.X64);
                break;
              case IntellijJsonRelease.KEY_MAC_ARM:
                addVersionEachOs(urlVersion, downloads, os, OperatingSystem.MAC, SystemArchitecture.ARM64);
                break;
            }
          }
          urlVersion.save();
        } catch (Exception e) {
          logger.error("For tool {} we failed to add version {}.", toolWithEdition, version, e);
        }
      }
    }
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    throw new IllegalStateException();
  }

  /**
   * Get link and link for the checksum for each OS, which are separate nodes in the json
   */
  private void addVersionEachOs(UrlVersion url, Map<String, IntellijJsonDownloadsItem> downloads, String jsonOS,
      OperatingSystem os, SystemArchitecture systemArchitecture) {

    IntellijJsonDownloadsItem downloadItem = downloads.get(jsonOS);
    String link = downloadItem.getLink();
    String checkSumLink = downloadItem.getChecksumLink();
    if (checkSumLink.isEmpty()) {
      doAddVersion(url, link, os, systemArchitecture);
    } else {
      String cs = getCheckSum(checkSumLink);
      doAddVersion(url, link, os, systemArchitecture, cs);
    }

  }

  /**
   * Follows link and gets body as string which contains checksum
   */
  private String getCheckSum(String checksumLink) {

    String responseCS = doGetResponseBodyAsString(checksumLink);
    return responseCS.split(" ")[0];
  }

  @Override
  protected String getTool() {

    return "intellij";
  }

  @Override
  protected String getEdition() {

    return "community";
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + JSON_URL;
  }

  /**
   * @return String of version base URL
   */
  protected String getVersionBaseUrl() {

    return VERSION_BASE_URL;
  }

  @Override
  protected Class<IntellijJsonObject> getJsonObjectType() {

    return IntellijJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(IntellijJsonObject jsonItem, Collection<String> versions) {

    throw new IllegalStateException();
  }
}
