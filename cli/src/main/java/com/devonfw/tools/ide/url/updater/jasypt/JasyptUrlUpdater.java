package com.devonfw.tools.ide.url.updater.jasypt;

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
  protected String getMavenGroupIdPath() {

    return "org/jasypt";
  }

  @Override
  protected String getMavenArtifcatId() {

    return "jasypt";
  }

}
