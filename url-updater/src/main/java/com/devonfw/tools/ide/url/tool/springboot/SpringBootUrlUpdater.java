package com.devonfw.tools.ide.url.tool.springboot;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;

/**
 * {@link MavenBasedUrlUpdater} for spring-boot-cli
 */
public class SpringBootUrlUpdater extends MavenBasedUrlUpdater {

  @Override
  protected String getTool() {

    return "springboot";
  }

  @Override
  protected String getExtension() {

    return "-bin.tar.gz";
  }

  @Override
  protected String getMavenGroupIdPath() {

    return "org/springframework/boot";
  }

  @Override
  protected String getMavenArtifactId() {

    return "spring-boot-cli";
  }
}
