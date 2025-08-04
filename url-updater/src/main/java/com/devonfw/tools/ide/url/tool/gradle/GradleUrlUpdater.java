package com.devonfw.tools.ide.url.tool.gradle;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Gradle.
 */
public class GradleUrlUpdater extends WebsiteUrlUpdater {

  private static final String HASHSUM_GRAB_PATTERN = "((.*)\\s){5}";

  private static final Pattern VERSION_PATTERN = Pattern.compile("release-checksums#v(\\d\\.\\d[\\.\\d]*)\"");
  
  private static final Pattern SHA256_PATTERN = Pattern.compile("[a-fA-F0-9]{64}");

  private String responseBody;

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/releases";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://gradle.org";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://services.gradle.org";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  protected String getTool() {

    return "gradle";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    if (this.responseBody == null) {
      this.responseBody = doGetResponseBodyAsString(getVersionBaseUrl() + "/release-checksums");
    }

    String hashSum = "";
    if (this.responseBody != null && !this.responseBody.isEmpty()) {
      hashSum = doGetHashSumForVersion(this.responseBody, urlVersion.getName());
    }

    String downloadUrl = getDownloadBaseUrl() + "/distributions/gradle-" + urlVersion.getName() + "-bin.zip";
    if (hashSum.isEmpty()) {
      doAddVersion(urlVersion, downloadUrl);
    } else {
      doAddVersion(urlVersion, downloadUrl, null, null, hashSum);
    }

  }

  @Override
  protected String mapVersion(String version) {

    version = version.replace("release-checksums#v", "").replace("\"", "");
    return super.mapVersion(version);
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
      var hashMatcher = SHA256_PATTERN.matcher(versionMatch);
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
      addVersion(version, versions);
    }
    return versions;
  }


  @Override
  public String getCpeVendor() {
    return "gradle";
  }

  @Override
  public String getCpeProduct() {
    return "gradle";
  }
}
