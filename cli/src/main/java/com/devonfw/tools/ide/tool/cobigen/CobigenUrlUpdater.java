package com.devonfw.tools.ide.tool.cobigen;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;

/**
 * {@link MavenBasedUrlUpdater} for cobigen-cli.
 */
public class CobigenUrlUpdater extends MavenBasedUrlUpdater {
  @Override
  protected String getTool() {

    return "cobigen";
  }

  @Override
  protected String getEdition() {

    return getTool();
  }

  @Override
  protected String getMavenGroupIdPath() {

    return "com/devonfw/cobigen";
  }

  @Override
  protected String getMavenArtifcatId() {

    return "cli";
  }
}
