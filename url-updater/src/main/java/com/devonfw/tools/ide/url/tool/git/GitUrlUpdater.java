package com.devonfw.tools.ide.url.tool.git;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Git.
 */
public class GitUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The constructor.
   */
  public GitUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link GitUrlUpdater}.
   *
   * @param downloadBaseUrl mock URL used as download base.
   * @param versionBaseUrl mock URL used as version base.
   */
  GitUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {
    return "git";
  }

  @Override
  protected String getGithubOrganization() {
    return "git-for-windows";
  }

  @Override
  protected String getGithubRepository() {
    return "git";
  }

  @Override
  public String mapVersion(String version) {

    // The git-for-windows repository publishes several products (e.g. MinGit and PortableGit) alongside the full installer.
    // We only track the "Git for Windows" releases and ignore everything else.
    if (!version.startsWith("Git for Windows")) {
      return null;
    }

    // Git for Windows release names are inconsistent, e.g.
    // "Git for Windows v2.56.0.windows.2", "Git for Windows 2.55.0(4)"
    // and "Git for Windows 2.49.1".
    version = version
        .replaceFirst(".*?(?=\\d)", "")
        .replaceFirst("\\.windows\\.", ".")
        .replace("(", ".")
        .replace(")", "");

    return super.mapVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();

    int lastDot = version.lastIndexOf('.');
    String gitVersion = version.substring(0, lastDot);
    String windowsRevision = version.substring(lastDot + 1);

    String releaseVersion = "v" + gitVersion + ".windows." + windowsRevision;
    String baseUrl = createGithubReleaseDownloadUrl(releaseVersion, "");

    // On GitHub the build revision is only part of the file name once it is larger than 1 (e.g. "Git-2.56.0-64-bit.exe" for windows.1 but
    // "Git-2.56.0.2-64-bit.exe" for windows.2)
    String fileNameVersion = "1".equals(windowsRevision) ? gitVersion : version;

    doAddVersion(
        urlVersion,
        baseUrl + "Git-" + fileNameVersion + "-64-bit.exe",
        WINDOWS,
        X64);

    doAddVersion(
        urlVersion,
        baseUrl + "Git-" + fileNameVersion + "-arm64.exe",
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
