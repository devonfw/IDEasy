package com.devonfw.tools.ide.url.tool.ollama;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

/**
 * {@link GithubUrlReleaseUpdater} for Ollama.
 */
public class OllamaUrlUpdater extends GithubUrlReleaseUpdater {

  /**
   * The Constructor.
   */
  public OllamaUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link OllamaUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used as download base.
   * @param versionBaseUrl mock url used as version base.
   */
  OllamaUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "ollama";
  }

  @Override
  protected String getGithubOrganization() {

    return "ollama";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "ollama-");
    // darwin ships a single binary for both x64 and arm64
    doAddVersion(urlVersion, baseUrl + "darwin.tgz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin.tgz", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "windows-amd64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "windows-arm64.zip", WINDOWS, ARM64);
    // linux assets are distributed as .tar.zst (zstd) which IDEasy cannot extract yet
    // (see com.devonfw.tools.ide.io.TarCompression), hence they are intentionally not added
  }

  @Override
  public String getCpeVendor() {

    return "ollama";
  }

  @Override
  public String getCpeProduct() {

    return "ollama";
  }
}
