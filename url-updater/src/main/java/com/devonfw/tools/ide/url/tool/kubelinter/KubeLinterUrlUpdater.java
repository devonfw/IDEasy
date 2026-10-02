package com.devonfw.tools.ide.url.tool.kubelinter;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for GitHub <a href="https://github.com/stackrox/kube-linter">KubeLinter</a>.
 * <p>
 * Follows the official release asset structure of the kube-linter repository:
 * <a href="https://github.com/stackrox/kube-linter">https://github.com/stackrox/kube-linter</a>.
 * <p>
 * Download URL pattern: https://github.com/stackrox/kube-linter/releases/download/v${version}/kube-linter-${os}[_arm64].tar.gz
 * <br>
 * Examples:
 * <ul>
 * <li><a
 * href="https://github.com/stackrox/kube-linter/releases/download/v0.8.3/kube-linter-linux_arm64.tar.gz">github.com/stackrox/kube-linter/releases/download/v0.8.3/kube-linter-linux_arm64.tar.gz</a></li>
 * <li><a
 * href="https://github.com/stackrox/kube-linter/releases/download/v0.8.3/kube-linter-windows.tar.gz">github.com/stackrox/kube-linter/releases/download/v0.8.3/kube-linter-windows.tar.gz</a></li>
 * </ul>
 */
public class KubeLinterUrlUpdater extends GithubUrlReleaseUpdater {

  /** The minimum version for which all six platform/architecture release assets are available. */
  private static final VersionIdentifier MIN_KUBE_LINTER_VID = VersionIdentifier.of("0.7.0");

  /**
   * The constructor.
   */
  public KubeLinterUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link KubeLinterUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  KubeLinterUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  protected String getGithubOrganization() {

    return "stackrox";
  }

  @Override
  public String getTool() {

    return "kube-linter";
  }

  @Override
  protected String getGithubRepository() {

    return "kube-linter";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.isGreaterOrEqual(MIN_KUBE_LINTER_VID)) {

      String baseUrl = createGithubReleaseDownloadUrl("v${version}", "kube-linter-");

      doAddVersion(urlVersion, baseUrl + "linux.tar.gz", LINUX);
      doAddVersion(urlVersion, baseUrl + "linux_arm64.tar.gz", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "darwin.tar.gz", MAC);
      doAddVersion(urlVersion, baseUrl + "darwin_arm64.tar.gz", MAC, ARM64);
      doAddVersion(urlVersion, baseUrl + "windows.tar.gz", WINDOWS);
      doAddVersion(urlVersion, baseUrl + "windows_arm64.tar.gz", WINDOWS, ARM64);
    }
  }
}
