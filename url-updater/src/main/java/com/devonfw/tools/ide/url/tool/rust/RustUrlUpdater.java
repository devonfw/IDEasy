package com.devonfw.tools.ide.url.tool.rust;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for Rust installed via rustup script. Gets version information from GitHub rust-lang/rustup releases, but downloads the generic
 * rustup installer script.
 */
public class RustUrlUpdater extends GithubUrlTagUpdater {

  private static final String RUSTUP_REPO = "rustup";
  private static final String RUSTUP_SCRIPT_URL = "https://sh.rustup.rs";

  @Override
  public String getTool() {

    return "rust";
  }

  @Override
  protected String getGithubOrganization() {

    return "rust-lang";
  }

  @Override
  protected String getGithubRepository() {

    return RUSTUP_REPO;
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  public String mapVersion(String version) {

    String mappedVersion = super.mapVersion(version);
    if (mappedVersion == null) {
      return null;
    }
    if (!mappedVersion.matches("\\d+\\.\\d+\\.\\d+")) {
      return null;
    }
    return mappedVersion;
  }

  @Override
  protected String getDownloadBaseUrl() {

    return RUSTUP_SCRIPT_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    // Download the generic rustup script (not version-specific)
    doAddVersion(urlVersion, getDownloadBaseUrl());
  }

  @Override
  protected boolean isOsDependent() {

    return false;
  }

  @Override
  protected boolean isValidContentType(String contentType) {

    return super.isValidContentType(contentType) || contentType.startsWith("text/plain") || contentType.startsWith("text/x-shellscript");
  }
}

