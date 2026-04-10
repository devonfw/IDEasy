package com.devonfw.tools.ide.url.tool.squirrelsql;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for SQuirreL SQL.
 */
public class SquirrelSqlUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("4.4.0");

  @Override
  public String getTool() {

    return "squirrel-sql";
  }

  @Override
  protected String getGithubOrganization() {
    return "squirrel-sql-client";
  }

  @Override
  protected String getGithubRepository() {

    return "squirrel-sql-stable-releases";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://github.com/squirrel-sql-client/squirrel-sql-stable-releases/releases/download";
  }

  @Override
  public String mapVersion(String version) {

    // Squirrel sql versions on GitHub have the suffix "-installer" or "-a_plainzip".
    // We only want the plainzip versions and not the installer. Also, some versions strangely have a leading whitespace...
    if (version.contains("a_plainzip")) {
      return super.mapVersion(version.split("-", 2)[0].replaceAll("\\s", ""));
    } else {
      return null;
    }
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();
    VersionComparisonResult versionComparisonResult = versionIdentifier.compareVersion(MIN_VERSION);
    if (versionComparisonResult.isEqual() || versionComparisonResult.isGreater()) {
      doAddVersion(urlVersion, getDownloadBaseUrl() + "/${version}-a_plainzip/squirrelsql-${version}-optional.zip");
    }
  }
}
