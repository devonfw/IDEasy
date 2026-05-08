package com.devonfw.tools.ide.url.tool.inso;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for GitHub Insomnia CLI.
 * <p>
 * Follows the official installation structure from GitHub's insomnia repository: <a href="https://github.com/Kong/insomnia">https://github.com/Kong/insomnia</a>.
 * <p>
 * Download URL pattern: https://github.com/Kong/insomnia/releases/download/core@${version}/inso-${os}-${version}.${ext}
 * Examples:
 * - <a href="https://github.com/Kong/insomnia/releases/download/core@12.5.0/inso-linux-x64-12.5.0.tar.xz">github.com/Kong/insomnia/releases/download/core@12.5.0/inso-linux-x64-12.5.0.tar.xz</a>
 * - <a href="https://github.com/Kong/insomnia/releases/download/core@12.5.0-beta.0/inso-windows-12.5.0-beta.0.zip">github.com/Kong/insomnia/releases/download/core@12.5.0-beta.0/inso-windows-12.5.0-beta.0.zip</a>
 */
public class InsoUrlUpdater extends GithubUrlTagUpdater {

  private static final String BASE_URL = "https://github.com";
   private static final VersionIdentifier MIN_INSO_VID = VersionIdentifier.of("11.5.0");

  @Override
  protected String getGithubOrganization() {
    return "Kong";
  }

  @Override
  public String getTool() {
    return "inso";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return BASE_URL;
  }

  @Override
  protected String getGithubRepository() {
    return "insomnia";
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "core@";
  }

  @Override
  public String getCpeVendor() {
    return "konghq";
  }

  @Override
  public String getCpeProduct() {
    return "insomnia";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_INSO_VID).isGreater()) {
      //TODO: Refactor with createGithubReleaseDownloadUrl after pulling from main
     String baseUrl = getDownloadBaseUrl() + "/Kong/insomnia/releases/download/core@${version}/inso-";

    doAddVersion(urlVersion, baseUrl + "linux-x64-${version}.tar.xz", LINUX);
    doAddVersion(urlVersion, baseUrl + "macos-${version}.zip", MAC);
    doAddVersion(urlVersion, baseUrl + "windows-${version}.zip", WINDOWS);}
  }
}
