package com.devonfw.tools.ide.url.updater.graalvm;

import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * Abstract {@link GithubUrlUpdater} base-class for GraalVM editions.
 */
public abstract class GraalVmUrlUpdater extends GithubUrlUpdater {

  @Override
  protected String getTool() {

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

}
