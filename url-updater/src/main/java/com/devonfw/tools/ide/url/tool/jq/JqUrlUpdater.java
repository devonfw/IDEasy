package com.devonfw.tools.ide.url.tool.jq;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for GitHub <a href="https://github.com/jqlang/jq">jq</a>.
 * <p>
 * Follows the official release asset structure of the jq repository:
 * <a href="https://github.com/jqlang/jq">https://github.com/jqlang/jq</a>.
 * <p>
 * Download URL pattern: https://github.com/jqlang/jq/releases/download/jq-${version}/jq-${os}-${arch}(.exe for Windows)
 * <br>
 * Examples:
 * <ul>
 * <li><a
 * href="https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-linux-amd64">github.com/jqlang/jq/releases/download/jq-1.8.2/jq-linux-amd64</a></li>
 * <li><a
 * href="https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-windows-arm64.exe">github.com/jqlang/jq/releases/download/jq-1.8.2/jq-windows-arm64.exe</a></li>
 * </ul>
 */
public class JqUrlUpdater extends GithubUrlReleaseUpdater {

  /** The minimum version for which all six platform/architecture release assets (including {@code windows-arm64}) are available. */
  private static final VersionIdentifier MIN_JQ_VERSION_IDENTIFIER = VersionIdentifier.of("1.8.2");

  /**
   * The constructor.
   */
  public JqUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link JqUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  JqUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  protected String getGithubOrganization() {

    return "jqlang";
  }

  @Override
  public String getTool() {

    return "jq";
  }

  @Override
  protected String getGithubRepository() {

    return "jq";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "jq ";
  }

  @Override
  public String mapVersion(String version) {
    return super.mapVersion(version.trim());
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();

    if (versionIdentifier.isGreaterOrEqual(MIN_JQ_VERSION_IDENTIFIER)) {

      String baseUrl = createGithubReleaseDownloadUrl("jq-${version}", "jq-");

      doAddVersion(urlVersion, baseUrl + "linux-amd64", LINUX);
      doAddVersion(urlVersion, baseUrl + "linux-arm64", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "macos-amd64", MAC);
      doAddVersion(urlVersion, baseUrl + "macos-arm64", MAC, ARM64);
      doAddVersion(urlVersion, baseUrl + "windows-amd64.exe", WINDOWS);
      doAddVersion(urlVersion, baseUrl + "windows-arm64.exe", WINDOWS, ARM64);
    }
  }
}
