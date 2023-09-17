package com.devonfw.tools.ide.url.updater.gradle;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Gradle.
 */
public class GradleUrlUpdater extends WebsiteUrlUpdater {

  private final static String VERSION_PATTERN = "release-checksums#v(\\d\\.\\d[\\.\\d]*)\"";

  private final static String HASHSUM_GRAB_PATTERN = "((.*)\\s){5}";

  private final static String SHA256_PATTERN = "[a-fA-F0-9]{64}";

  private final static String VERSION_URL = "https://gradle.org/releases";

  private final static String HASH_VERSION_URL = "https://gradle.org/release-checksums";

  private final static String DOWNLOAD_URL = "https://services.gradle.org/distributions/gradle-${version}-bin.zip";

  private String responseBody;

  @Override
  protected String getVersionUrl() {

    return VERSION_URL;
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile(VERSION_PATTERN);
  }

  @Override
  protected String getTool() {

    return "gradle";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    if (responseBody == null) {
      responseBody = doGetResponseBodyAsString(HASH_VERSION_URL);
    }

    String hashSum = "";
    if (responseBody != null && !responseBody.isEmpty()) {
      hashSum = doGetHashSumForVersion(responseBody, urlVersion.getName());
    }

    if (hashSum.isEmpty()) {
      doAddVersion(urlVersion, DOWNLOAD_URL);
    } else {
      doAddVersion(urlVersion, DOWNLOAD_URL, null, null, hashSum);
    }

  }

  /**
   * Gets a hashSum from the release-checksum page for the provided version
   *
   * @param htmlBody the body of the hashSum HTML page
   * @param version the version
   * @return the checksum
   */
  protected String doGetHashSumForVersion(String htmlBody, String version) {

    String regexVersion = version.replace(".", "\\.");
    Pattern hashVersionPattern = Pattern.compile("v" + regexVersion + HASHSUM_GRAB_PATTERN);
    var matcher = hashVersionPattern.matcher(htmlBody);
    if (matcher.find()) {
      String versionMatch = matcher.group();
      Pattern hashPattern = Pattern.compile(SHA256_PATTERN);
      var hashMatcher = hashPattern.matcher(versionMatch);
      if (hashMatcher.find()) {
        return hashMatcher.group();
      }
    }
    return "";
  }

  @Override
  protected Set<String> doGetRegexMatchesAsList(String htmlBody) {

    Set<String> versions = new HashSet<>();
    var matcher = getVersionPattern().matcher(htmlBody);
    while (matcher.find()) {
      String version = matcher.group();
      String newVersion = version.replace("release-checksums#v", "").replace("\"", "");
      addVersion(newVersion, versions);
    }
    return versions;
  }

}
