package com.devonfw.tools.ide.tool.squirrelsql;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for SquirrelSQL
 */
public class SquirrelSqlUrlUpdater extends GithubUrlUpdater {

  public static final VersionIdentifier MIN_SQUIRRELSQL_VID = VersionIdentifier.of("4.4.0");

  @Override
  protected String getTool() {

    return "squirrelsql";
  }

  @Override
  protected String getGithubOrganization() {

    return "squirrel-sql-client";
  }

  @Override
  protected String getGithubRepository() {

    return "squirrel-sql-code";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    if (vid.compareVersion(MIN_SQUIRRELSQL_VID).isGreater()) {
      String baseUrl = "https://squirrel-sql.sourceforge.io/#installation";
      doAddVersion(urlVersion, baseUrl + ".zip");
      doAddVersion(urlVersion, baseUrl + ".tar.gz");
    }
  }

}

