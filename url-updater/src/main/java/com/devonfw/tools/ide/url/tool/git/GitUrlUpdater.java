package com.devonfw.tools.ide.url.tool.git;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for git.
 */
public class GitUrlUpdater extends WebsiteUrlUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://github.com/git-for-windows/git";
  private static final String VERSION_BASE_URL = "https://git-scm.com";

  private static final Pattern VERSION_PATTERN =
      Pattern.compile("(\\d+\\.\\d+\\.\\d+)\\((\\d+)\\)");

  /**
   * The constructor.
   */
  public GitUrlUpdater() {
    super(DOWNLOAD_BASE_URL, VERSION_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link GitUrlUpdater}.
   *
   * @param baseUrl mock URL used as download and version base.
   */
  GitUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {
    return "git";
  }

  @Override
  protected String getVersionUrl() {
    return getVersionBaseUrl() + "/install/windows";
  }

  @Override
  protected Pattern getVersionPattern() {
    return VERSION_PATTERN;
  }

  @Override
  public String mapVersion(String version) {

    return version
        .replace("(", ".")
        .replace(")", "");
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();

    int lastDot = version.lastIndexOf('.');
    String gitVersion = version.substring(0, lastDot);
    String windowsRevision = version.substring(lastDot + 1);

    String releaseUrl = getDownloadBaseUrl()
        + "/releases/download/v"
        + gitVersion
        + ".windows."
        + windowsRevision
        + "/";

    doAddVersion(
        urlVersion,
        releaseUrl + "Git-" + version + "-64-bit.exe",
        WINDOWS,
        X64);

    doAddVersion(
        urlVersion,
        releaseUrl + "Git-" + version + "-arm64.exe",
        WINDOWS,
        ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "git-scm";
  }

  @Override
  public String getCpeProduct() {
    return "git";
  }
}
