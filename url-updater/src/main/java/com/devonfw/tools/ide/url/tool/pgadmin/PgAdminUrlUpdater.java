package com.devonfw.tools.ide.url.tool.pgadmin;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

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

    return "https://www.postgresql.org/ftp/pgadmin/pgadmin4/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("v(\\d{1,2}+\\.\\d+)");
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    String baseUrl = "https://ftp.postgresql.org/pub/pgadmin/pgadmin4/";
    doAddVersion(urlVersion, baseUrl + "v${version}/windows/pgadmin4-${version}-x64.exe", OperatingSystem.WINDOWS);

    if (vid.compareVersion(VersionIdentifier.of("7.6")).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "v${version}/macos/pgadmin4-${version}-arm64.dmg", MAC, ARM64);
      doAddVersion(urlVersion, baseUrl + "v${version}/macos/pgadmin4-${version}-x86_64.dmg", MAC, X64);
    } else {
      doAddVersion(urlVersion, baseUrl + "v${version}/macos/pgadmin4-${version}.dmg", MAC);
    }
  }

  @Override
  public String getCpeVendor() {
    return "pgadmin-org";
  }

  @Override
  public String getCpeProduct() {
    return "pgadmin";
  }

}
