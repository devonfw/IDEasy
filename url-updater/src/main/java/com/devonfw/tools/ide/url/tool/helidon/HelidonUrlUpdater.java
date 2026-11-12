package com.devonfw.tools.ide.url.tool.helidon;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link MavenBasedUrlUpdater} for the Helidon CLI.
 * <p>
 * The Helidon CLI is maintained by Oracle (https://helidon.io). Its versioned, OS-agnostic distribution is published on Maven Central as
 * {@code io.helidon.build-tools.cli:helidon-cli-impl}; the native single-file binaries on https://helidon.io only expose a {@code latest}
 * alias, so the Maven distribution is used to allow version-pinning.
 */
public class HelidonUrlUpdater extends MavenBasedUrlUpdater {

  /** The minimum supported version: only the current (4.x) line is tracked. */
  public static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("4.0.0");

  /**
   * The constructor.
   */
  public HelidonUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link HelidonUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used for download base.
   * @param versionBaseUrl mock url used for version base.
   */
  HelidonUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {
    return "helidon";
  }

  @Override
  protected String getMavenGroupIdPath() {
    return "io/helidon/build-tools/cli";
  }

  @Override
  protected String getMavenArtifcatId() {
    return "helidon-cli-impl";
  }

  @Override
  protected String getExtension() {
    return ".zip";
  }

  @Override
  public boolean isValidVersion(String version) {

    VersionIdentifier artifactVersion = VersionIdentifier.of(version);
    if (artifactVersion != null) {
      return artifactVersion.isGreaterOrEqual(MIN_VERSION);
    }
    return false;
  }

  @Override
  protected boolean isOsDependent() {
    return false;
  }

  @Override
  public String getCpeVendor() {
    return "oracle";
  }

  @Override
  public String getCpeProduct() {
    return "helidon";
  }
}
