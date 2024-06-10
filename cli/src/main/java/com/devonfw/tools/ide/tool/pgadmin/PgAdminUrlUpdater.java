package com.devonfw.tools.ide.tool.pgadmin;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

import java.util.regex.Pattern;

/**
 * {@link WebsiteUrlUpdater} for pgadmin.
 */
public class PgAdminUrlUpdater extends WebsiteUrlUpdater {

  @Override
  protected String getTool() {

    return "pgadmin";
  }

  @Override
  protected String getVersionUrl() {

    return "https://www.pgadmin.org/docs/pgadmin4/latest/release_notes.html";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("Version (\\d{1,2}+\\.\\d+)");
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        "https://ftp.postgresql.org/pub/pgadmin/pgadmin4/v${version}/windows/pgadmin4-${version}-x64.exe",
        OperatingSystem.WINDOWS);
  }

}
