package com.devonfw.tools.ide.url.tool;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;

public class RubyUrlUpdater extends GithubUrlReleaseUpdater {


  @Override
  public String getTool() {
    return "ruby";
  }

  @Override
  protected String getGithubOrganization() {
    return "ruby";
  }

  @Override
  protected String getGithubRepository() {
    return "ruby";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl()
  }
}
