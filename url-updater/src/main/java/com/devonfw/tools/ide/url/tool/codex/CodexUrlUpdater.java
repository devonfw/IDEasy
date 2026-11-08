package com.devonfw.tools.ide.url.tool.codex;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for the OpenAI Codex CLI. The openai/codex repo also releases a Python SDK and voice builds, so we only take releases
 * named as a bare version (the Rust CLI), whose git tag is prefixed with {@code rust-v} (e.g. {@code rust-v0.154.0}).
 */
public class CodexUrlUpdater extends GithubUrlReleaseUpdater {

  // first release with all six OS/arch assets (Windows arm64 added in rust-v0.28.0)
  private static final VersionIdentifier MIN_CODEX_VID = VersionIdentifier.of("0.28.0");

  /**
   * The Constructor.
   */
  public CodexUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link CodexUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  CodexUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {
    return "codex";
  }

  @Override
  protected String getGithubOrganization() {
    return "openai";
  }

  @Override
  protected String getGithubRepository() {
    return "codex";
  }

  @Override
  protected String doGetVersionUrl() {
    // openai/codex publishes many releases per day, so request a full page to keep recent stable versions in view.
    return super.doGetVersionUrl() + "?per_page=100";
  }

  @Override
  public String mapVersion(String version) {
    // keep only the Rust CLI releases, which are named as a bare version like "0.154.0"
    if (!version.matches("\\d+\\.\\d+\\.\\d+.*")) {
      return null;
    }
    return super.mapVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("rust-v${version}", "codex-");
    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (!vid.isLess(MIN_CODEX_VID)) {

      doAddVersion(urlVersion, baseUrl + "x86_64-unknown-linux-musl.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "aarch64-unknown-linux-musl.tar.gz", LINUX, ARM64);

      doAddVersion(urlVersion, baseUrl + "x86_64-apple-darwin.tar.gz", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "aarch64-apple-darwin.tar.gz", MAC, ARM64);

      doAddVersion(urlVersion, baseUrl + "x86_64-pc-windows-msvc.exe.zip", WINDOWS, X64);
      doAddVersion(urlVersion, baseUrl + "aarch64-pc-windows-msvc.exe.zip", WINDOWS, ARM64);
    }
  }
}
