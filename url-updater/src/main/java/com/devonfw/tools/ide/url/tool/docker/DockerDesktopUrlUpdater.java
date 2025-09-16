package com.devonfw.tools.ide.url.tool.docker;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link WebsiteUrlUpdater} for docker-desktop.
 */
public class DockerDesktopUrlUpdater extends WebsiteUrlUpdater {

  private final static Set<VersionIdentifier> WINDOWS_ONLY_VERSIONS = Set.of(VersionIdentifier.of("4.16.3"),
      VersionIdentifier.of("4.4.3"), VersionIdentifier.of("4.4.4"), VersionIdentifier.of("4.17.1"),
      VersionIdentifier.of("4.5.1"));

  private static final Pattern VERSION_PATTERN = Pattern.compile("(4\\.\\d{1,4}+\\.\\d+)");

  @Override
  protected String getTool() {

    return "docker";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = VersionIdentifier.of(urlVersion.getName());
    String version = urlVersion.getName().replaceAll("\\.", "");
    // get Code for version
    String body = downloadAsString(getVersionUrl());
    String regex = "href=#" + version
        // .......1.........................................................2.................
        + ".{8,12}(\r\n|\r|\n).{0,350}href=https://desktop\\.docker\\.com.*?(\\d{5,6}).*\\.exe";
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(body);
    String code;
    if (matcher.find()) {
      code = matcher.group(2);
      boolean success = doAddVersion(urlVersion,
          getDownloadBaseUrl() + "/win/main/amd64/" + code + "/Docker%20Desktop%20Installer.exe", WINDOWS);
      if (!success) {
        return;
      }
      if (WINDOWS_ONLY_VERSIONS.stream().noneMatch(i -> vid.compareVersion(i).isEqual())) {
        doAddVersion(urlVersion, getDownloadBaseUrl() + "/mac/main/amd64/" + code + "/Docker.dmg", MAC, X64);
        doAddVersion(urlVersion, getDownloadBaseUrl() + "/mac/main/arm64/" + code + "/Docker.dmg", MAC, ARM64);
      }
    }
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://desktop.docker.com";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/desktop/release-notes/";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://docs.docker.com";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  public String getCpeVendor() {
    return "docker";
  }

  @Override
  public String getCpeProduct() {
    return "docker";
  }
}
