package com.devonfw.tools.ide.url.tool.graalvm;

import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * Abstract {@link GithubUrlTagUpdater} base-class for GraalVM editions.
 */
public abstract class GraalVmUrlUpdater extends GithubUrlTagUpdater {

  @Override
  public String getTool() {

    return "graalvm";
  }

  @Override
  protected String getGithubOrganization() {

    return "graalvm";
  }

  @Override
  protected String getGithubRepository() {

    return "graalvm-ce-builds";
  }

  @Override
  public String getCpeVendor() {
    return "oracle";
  }

  @Override
  public String getCpeProduct() {
    return "graalvm";
  }
}
