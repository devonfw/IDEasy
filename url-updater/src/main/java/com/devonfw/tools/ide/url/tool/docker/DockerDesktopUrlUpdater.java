package com.devonfw.tools.ide.url.tool.docker;

import static java.lang.String.format;

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

  private final static String DOCKER_RELEASE_NOTES_URL = "https://docs.docker.com/desktop/release-notes/";

  private final static String REGEX_FOR_DOCKER_VERSION =
      "href=#%s"                                  // Find the href with the readable version - %s provided by urlVersion
          + ".{0,300}"                            // We have to look in close range for the next part (docker lists a summary at top. If this range is to big
          // we'll find the latest listed version with download links that doesn't match the version we are looking for
          + "href=https://desktop\\.docker\\.com" // Start of download link
          + ".*?"                                 // We don't care if its windows or mac - match as least as possible characters
          + "(\\d{5,6})";                         // Associated docker-version to readable version we are looking for
  private final static String REGEX_FOR_DOWNLOAD_URLS = "https://desktop.docker.com/%s/main/%s/%s/";
  private final static String WIN_VERSION = "win";
  private final static String MAC_VERSION = "mac";
  private final static String AMD_ARCH_TYPE = "amd64";
  private final static String ARM_ARCH_TYPE = "arm64";

  @Override
  protected String getTool() {

    return "docker";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = VersionIdentifier.of(urlVersion.getName());
    String version = urlVersion.getName().replaceAll("\\.", "");
    // get Code for version
    String body = doGetResponseBodyAsString(DOCKER_RELEASE_NOTES_URL);
    String regexForDockerVersion = format(REGEX_FOR_DOCKER_VERSION, version);
    Pattern patternForDockerVersion = Pattern.compile(regexForDockerVersion, Pattern.DOTALL);
    Matcher matcherForDockerVersion = patternForDockerVersion.matcher(body);
    String code;
    if (matcherForDockerVersion.find()) {
      code = matcherForDockerVersion.group(1);
      addVersionsForWindows(urlVersion, code, body);
      if (!WINDOWS_ONLY_VERSIONS.stream().anyMatch(i -> vid.compareVersion(i).isEqual())) {
        addVersionsForMac(urlVersion, code, body);
      }
    }
  }

  /**
   * Adds the windows versions for docker if they exist
   *
   * @param urlVersion the readable version e.g. 4332 (4.33.2)
   * @param dockerVersion the associated docker version to readable version e.g. 179689
   * @param body the html body to search in
   */
  private void addVersionsForWindows(UrlVersion urlVersion, String dockerVersion, String body) {
    boolean versionExists = checkIfVersionExists(dockerVersion, body, WIN_VERSION, AMD_ARCH_TYPE);
    if (versionExists) {
      doAddVersion(urlVersion, "https://desktop.docker.com/win/main/amd64/" + dockerVersion + "/Docker%20Desktop%20Installer.exe", WINDOWS, X64);
    }
    versionExists = checkIfVersionExists(dockerVersion, body, WIN_VERSION, ARM_ARCH_TYPE);
    if (versionExists) {
      doAddVersion(urlVersion, "https://desktop.docker.com/win/main/arm64/" + dockerVersion + "/Docker%20Desktop%20Installer.exe", WINDOWS, ARM64);
    }
  }

  /**
   * Adds the mac versions for docker if they exist
   *
   * @param urlVersion the readable version e.g. 4332 (4.33.2)
   * @param dockerVersion the associated docker version to readable version e.g. 179689
   * @param body the html body to search in
   */
  private void addVersionsForMac(UrlVersion urlVersion, String dockerVersion, String body) {
    boolean versionExists = checkIfVersionExists(dockerVersion, body, MAC_VERSION, AMD_ARCH_TYPE);
    if (versionExists) {
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/amd64/" + dockerVersion + "/Docker.dmg", MAC, X64);
    }
    versionExists = checkIfVersionExists(dockerVersion, body, MAC_VERSION, ARM_ARCH_TYPE);
    if (versionExists) {
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/arm64/" + dockerVersion + "/Docker.dmg", MAC, ARM64);
    }
  }

  /**
   * As docker is very inconsistent by releasing versions we have to check every single one if the download link exists to prevent failing downloads
   * (403-errors) E.g. for only amd64 windows download link provided- <a href="https://docs.docker.com/desktop/release-notes/#4241">4.24.1</a> E.g. for no
   * download links provided - <a href="https://docs.docker.com/desktop/release-notes/#4242">4.24.2</a> E.g. for only mac download links provided - <a
   * href="https://docs.docker.com/desktop/release-notes/#4361">4.36.1</a>
   *
   * @param dockerVersion the associated docker version to readable version e.g. 179689 (4.33.2)
   * @param body the html body to search in
   * @param osVersion the os versions - win or mac
   * @param archType the archType - amd64 or arm64
   * @return true if the version exists - false if not
   */
  private boolean checkIfVersionExists(String dockerVersion, String body, String osVersion, String archType) {
    String regexForDownloadUrlS = format(REGEX_FOR_DOWNLOAD_URLS, osVersion, archType, dockerVersion);
    Pattern patternForDownloadUrls = Pattern.compile(regexForDownloadUrlS, Pattern.DOTALL);
    Matcher matcherForDownloadUrls = patternForDownloadUrls.matcher(body);
    return matcherForDownloadUrls.find();
  }

  @Override
  protected String getVersionUrl() {

    return DOCKER_RELEASE_NOTES_URL;
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(4\\.\\d{1,4}+\\.\\d+)");
  }
}
