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

  private static final Pattern VERSION_PATTERN = Pattern.compile("(4\\.\\d{1,4}+\\.\\d+)");

  private final static String AMD_ARCH_TYPE = "amd64";
  private final static String ARM_ARCH_TYPE = "arm64";
  private final static String CHECKSUM_FILE = "checksums.txt";

  private final static String WIN_VERSION = "win";
  private final static String WIN_FILE = "Docker%20Desktop%20Installer.exe";
  private final static String WIN_BASE_URL = "/win/main/";
  private final static String WIN_AMD_URL = WIN_BASE_URL + AMD_ARCH_TYPE + "/";
  private final static String WIN_ARM_URL = WIN_BASE_URL + ARM_ARCH_TYPE + "/";

  private final static String MAC_VERSION = "mac";
  private final static String MAC_FILE = "Docker.dmg";
  private final static String MAC_BASE_URL = "/mac/main/";
  private final static String MAC_AMD_URL = MAC_BASE_URL + AMD_ARCH_TYPE + "/";
  private final static String MAC_ARM_URL = MAC_BASE_URL + ARM_ARCH_TYPE + "/";

  private final static String REGEX_FOR_DOWNLOAD_URLS = "/%s/main/%s/%s/";
  private final static String REGEX_FOR_DOCKER_VERSION =
      "href=#%s"                                  // Find the href with the readable version - %s provided by urlVersion
          + ".{0,300}"                            // We have to look in close range for the next part (docker lists a summary at top. If this range is too big
          // we'll find the latest listed version with download links that doesn't match the version we are looking for
          + "href=(\")?%s"                         // Start of download link
          + ".*?"                                 // We don't care if its windows or mac - match as least as possible characters
          + "(\\d{5,6})";                         // Associated docker-version to readable version we are looking for


  @Override
  protected String getTool() {

    return "docker";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = VersionIdentifier.of(urlVersion.getName());
    String version = urlVersion.getName().replaceAll("\\.", "");
    // get Code for version
    String body = doGetResponseBodyAsString(getVersionUrl());
    String regexForDockerVersion = format(REGEX_FOR_DOCKER_VERSION, version, getDownloadBaseUrl());
    Pattern patternForDockerVersion = Pattern.compile(regexForDockerVersion, Pattern.DOTALL);
    Matcher matcherForDockerVersion = patternForDockerVersion.matcher(body);
    String code;
    if (matcherForDockerVersion.find()) {
      code = matcherForDockerVersion.group(2);
      boolean success;
      success = addVersionsForWindows(urlVersion, code, body);
      if (!success) {
        return;
      }
      if (WINDOWS_ONLY_VERSIONS.stream().noneMatch(i -> vid.compareVersion(i).isEqual())) {
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
   * @return {@code true} if version was added successfully, {@code false} if not.
   */
  private boolean addVersionsForWindows(UrlVersion urlVersion, String dockerVersion, String body) {
    boolean versionExists = checkIfVersionExists(dockerVersion, body, WIN_VERSION, AMD_ARCH_TYPE);
    boolean success = false;
    if (versionExists) {
      String cs = getChecksum(getDownloadBaseUrl() + WIN_AMD_URL + dockerVersion + "/" + CHECKSUM_FILE);
      success = doAddVersion(urlVersion, getDownloadBaseUrl() + WIN_AMD_URL + dockerVersion + "/" + WIN_FILE, WINDOWS, X64, cs);
    }
    versionExists = checkIfVersionExists(dockerVersion, body, WIN_VERSION, ARM_ARCH_TYPE);
    if (versionExists) {
      String cs = getChecksum(getDownloadBaseUrl() + WIN_ARM_URL + dockerVersion + "/" + CHECKSUM_FILE);
      success = doAddVersion(urlVersion, getDownloadBaseUrl() + WIN_ARM_URL + dockerVersion + "/" + WIN_FILE, WINDOWS, ARM64, cs);
    }
    return success;
  }

  /**
   * Adds the mac versions for docker if they exist
   *
   * @param urlVersion the readable version e.g. 4332 (4.33.2)
   * @param dockerVersion the associated docker version to readable version e.g. 179689
   * @param body the html body to search in
   * @return {@code true} if version was added successfully, {@code false} if not.
   */
  private boolean addVersionsForMac(UrlVersion urlVersion, String dockerVersion, String body) {
    boolean versionExists = checkIfVersionExists(dockerVersion, body, MAC_VERSION, AMD_ARCH_TYPE);
    boolean success = false;
    if (versionExists) {
      String cs = getChecksum(getDownloadBaseUrl() + MAC_AMD_URL + dockerVersion + "/" + CHECKSUM_FILE);
      success = doAddVersion(urlVersion, getDownloadBaseUrl() + MAC_AMD_URL + dockerVersion + "/" + MAC_FILE, MAC, X64, cs);
    }
    versionExists = checkIfVersionExists(dockerVersion, body, MAC_VERSION, ARM_ARCH_TYPE);
    if (versionExists) {
      String cs = getChecksum(getDownloadBaseUrl() + MAC_ARM_URL + dockerVersion + "/" + CHECKSUM_FILE);
      success = doAddVersion(urlVersion, getDownloadBaseUrl() + MAC_ARM_URL + dockerVersion + "/" + MAC_FILE, MAC, ARM64, cs);
    }
    return success;
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
    String regexForDownloadUrlS = format(getDownloadBaseUrl() + REGEX_FOR_DOWNLOAD_URLS, osVersion, archType, dockerVersion);
    Pattern patternForDownloadUrls = Pattern.compile(regexForDownloadUrlS, Pattern.DOTALL);
    Matcher matcherForDownloadUrls = patternForDownloadUrls.matcher(body);
    return matcherForDownloadUrls.find();
  }

  /**
   * Retrieves the checksum for the passed url
   *
   * @param url url for specific version to download
   * @return the checksum in string format
   */
  private String getChecksum(String url) {
    String checksumAsString = doGetResponseBodyAsString(url);
    // Example checksum response: e832d4c2c99300436096b2e990220068e69ede845137a9dd63eff0a51e8a14e9 *Docker Desktop Installer.exe
    return checksumAsString.split(" ")[0];
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
