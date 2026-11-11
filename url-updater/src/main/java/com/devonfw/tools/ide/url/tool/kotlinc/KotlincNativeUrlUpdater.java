package com.devonfw.tools.ide.url.tool.kotlinc;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for kotlinc-native.
 * <p>
 * Since Kotlin 2.0 the release assets are named <code>kotlin-native-prebuilt-&lt;version&gt;-&lt;os&gt;-&lt;arch&gt;</code> (the previous
 * <code>kotlin-native-&lt;os&gt;-&lt;arch&gt;-&lt;version&gt;</code> assets are no longer published). The prebuilt assets also exist for the last 1.9.x
 * release, so this naming stays compatible with existing versions.
 */
public class KotlincNativeUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The Constructor.
   */
  public KotlincNativeUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link KotlincNativeUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  KotlincNativeUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {

    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  protected String getGithubOrganization() {

    return "JetBrains";
  }

  @Override
  protected String getGithubRepository() {

    return "kotlin";
  }

  @Override
  public String getTool() {

    return "kotlinc-native";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "Kotlin ";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getGithubReleaseDownloadBaseUrl() + "/" + "v${version}/kotlin-native-prebuilt-${version}-";
    doAddVersion(urlVersion, baseUrl + "windows-x86_64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "linux-x86_64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "macos-x86_64.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "macos-aarch64.tar.gz", MAC, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "jetbrains";
  }

  @Override
  public String getCpeProduct() {
    return "kotlin";
  }
}
