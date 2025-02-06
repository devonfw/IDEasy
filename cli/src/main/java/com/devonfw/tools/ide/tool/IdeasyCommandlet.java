package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link MvnBasedLocalToolCommandlet} for IDEasy (ide-cli).
 */
public class IdeasyCommandlet extends MvnBasedLocalToolCommandlet {

  private static final MvnArtifact ARTIFACT = MvnArtifact.ofIdeasyCli("*!", "tar.gz", "${os}-${arch}");

  private static final VersionIdentifier LATEST_SNAPSHOT = VersionIdentifier.of("*-SNAPSHOT");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public IdeasyCommandlet(IdeContext context) {

    super(context, "ideasy", ARTIFACT, Set.of(Tag.PRODUCTIVITY, Tag.IDE));
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return IdeVersion.getVersionIdentifier();
  }

  @Override
  public String getConfiguredEdition() {

    return this.tool;
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    if (IdeVersion.getVersionString().contains("SNAPSHOT")) {
      return LATEST_SNAPSHOT;
    }
    if (IdeVersion.getVersionIdentifier().getDevelopmentPhase().isStable()) {
      return VersionIdentifier.LATEST;
    } else {
      return VersionIdentifier.LATEST_UNSTABLE;
    }
  }

  @Override
  public Path getToolPath() {

    return this.context.getIdeInstallationPath();
  }

  @Override
  public boolean install(boolean silent) {

    if (IdeVersion.isUndefined()) {
      this.context.warning("You are using IDEasy version {} what indicates local development - skipping upgrade.", IdeVersion.getVersionString());
      return false;
    }
    return super.install(silent);
  }

  /**
   * @return the latest released {@link VersionIdentifier version} of IDEasy.
   */
  public VersionIdentifier getLatestVersion() {

    VersionIdentifier currentVersion = IdeVersion.getVersionIdentifier();
    if (IdeVersion.isUndefined()) {
      return currentVersion;
    }
    VersionIdentifier configuredVersion = getConfiguredVersion();
    return getToolRepository().resolveVersion(this.tool, getConfiguredEdition(), configuredVersion, this);
  }

  /**
   * Checks if an update is available and logs according information.
   *
   * @return {@code true} if an update is available, {@code false} otherwise.
   */
  public boolean checkIfUpdateIsAvailable() {
    VersionIdentifier installedVersion = getInstalledVersion();
    VersionIdentifier latestVersion = getLatestVersion();
    if (installedVersion.equals(latestVersion)) {
      this.context.success("Your version of IDEasy is {} what is the latest released version.", installedVersion);
      return false;
    } else {
      this.context.interaction("Your version if IDEasy is {} but version {} is available. Please run the following command to upgrade to the latest version:\n"
          + "ide upgrade", installedVersion, latestVersion);
      return true;
    }
  }
}
