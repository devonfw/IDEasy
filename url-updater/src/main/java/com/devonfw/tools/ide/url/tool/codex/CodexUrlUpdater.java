package com.devonfw.tools.ide.url.tool.codex;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for OpenAI Codex CLI.
 * <p>
 * The {@code openai/codex} repository publishes releases for several components (the Rust CLI, the Python SDK and voice builds). Only the Rust CLI releases are
 * relevant here. Their {@link com.devonfw.tools.ide.github.GithubRelease#name() release name} is a bare version such as {@code 0.154.0}, while the
 * corresponding git tag is prefixed with {@code rust-v} (e.g. {@code rust-v0.154.0}).
 * <p>
 * Download URL pattern: https://github.com/openai/codex/releases/download/rust-v${version}/codex-${arch}-${os}.${ext}
 */
public class CodexUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_CODEX_VID = VersionIdentifier.of("0.112.0");

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
  public String mapVersion(String version) {
    // Codex publishes releases for several components; only the Rust CLI releases are named as a bare version like "0.154.0".
    // This filters out foreign releases such as the Python SDK ("Python SDK 0.154.0") or voice builds.
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
