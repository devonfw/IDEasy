package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;

/**
 * {@link MavenBasedUrlUpdater} for jasypt
 */
public class JasyptUrlUpdater extends MavenBasedUrlUpdater {
  @Override
  protected String getTool() {

    return "jasypt";
  }

  @Override
  protected String getEdition() {

    return getTool();
  }

  @Override
  protected String getMavenGroupIdPath() {

    return "org/jasypt";
  }

  @Override
  protected String getMavenArtifcatId() {

    return "jasypt";
  }

}
