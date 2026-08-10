package com.devonfw.tools.ide.url.tool.ruby;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Ruby releases from jdx on GitHub.
 */
public class RubyJdxUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The constructor.
   */
  public RubyJdxUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link RubyJdxUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used as download base.
   * @param versionBaseUrl mock url used as version base.
   */
  RubyJdxUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {

    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "ruby";
  }

  @Override
  protected String getGithubOrganization() {

    return "jdx";
  }

  @Override
  protected String getGithubRepository() {

    return "ruby";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getVersionIdentifier().toString();
    String rubyVersion = removeBuildRevision(version);
    String baseUrl = createGithubReleaseDownloadUrl("${version}", "ruby-" + rubyVersion);

    doAddVersion(urlVersion, baseUrl + ".x86_64_linux.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + ".arm64_linux.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + ".macos.tar.gz", MAC, ARM64);
  }

  private String removeBuildRevision(String version) {

    return version.replaceFirst("-(\\d+)$", "");
  }

  @Override
  public String getCpeVendor() {

    return "ruby-lang";
  }

  @Override
  public String getCpeProduct() {

    return "ruby";
  }
}
