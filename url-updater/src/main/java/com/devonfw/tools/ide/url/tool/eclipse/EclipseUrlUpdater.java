package com.devonfw.tools.ide.url.tool.eclipse;

import java.util.List;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionPhase;
import com.devonfw.tools.ide.version.VersionSegment;

/**
 * Abstract {@link WebsiteUrlUpdater} base-class for eclipse editions.
 */
public abstract class EclipseUrlUpdater extends WebsiteUrlUpdater {

  private static final String BASE_URL = "https://download.eclipse.org/technology/epp/downloads";

  private static final List<String> MIRRORS = List.of(
      BASE_URL,
      "https://archive.eclipse.org/technology/epp/downloads",
      "https://ftp.osuosl.org/pub/eclipse/technology/epp/downloads");

  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d{4}-\\d{2}");

  @Override
  public String getTool() {

    return "eclipse";
  }

  /**
   * @return the eclipse edition name. May be different from {@link #getEdition()} allowing a different edition name (e.g. eclipse) for IDEasy.
   */
  protected String getEclipseEdition() {

    return getEdition();
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    // archive
    String releaseType = "R";
    VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();
    String version = urlVersion.getName();
    VersionSegment segment = versionIdentifier.getStart();
    while (segment != null) {
      if ((segment.getPhase() == VersionPhase.MILESTONE) || (segment.getPhase() == VersionPhase.RELEASE_CANDIDATE)) {
        // found a non release type (e.g. M1, M2, RC1, ...)
        releaseType = segment.getLettersString() + segment.getDigits();
        version = version.replace(segment.toString(), "");
        break;
      }
      segment = segment.getNextOrNull();
    }
    String edition = getEclipseEdition();
    for (String mirror : getMirrors()) {
      String baseUrl = mirror + "/release/" + version + "/" + releaseType + "/eclipse-" + edition + "-" + version + "-"
          + releaseType + "-";
      doUpdateVersions(urlVersion, baseUrl);
    }
  }

  @Override
  protected String getDownloadBaseUrl() {

    return BASE_URL;
  }

  protected List<String> getMirrors() {

    String downloadBaseUrl = getDownloadBaseUrl();
    if (!BASE_URL.equals(downloadBaseUrl)) {
      return List.of(downloadBaseUrl);
    }
    return List.of(
        BASE_URL,
        "https://archive.eclipse.org/technology/epp/downloads",
        "https://ftp.osuosl.org/pub/eclipse/technology/epp/downloads");
  }

  private boolean doUpdateVersions(UrlVersion urlVersion, String baseUrl) {

    boolean ok;
    ok = doAddVersion(urlVersion, baseUrl + "win32-x86_64.zip", WINDOWS, X64);
    if (!ok) {
      return false;
    }
    ok = doAddVersion(urlVersion, baseUrl + "linux-gtk-x86_64.tar.gz", LINUX, X64);
    ok = doAddVersion(urlVersion, baseUrl + "linux-gtk-aarch64.tar.gz", LINUX, ARM64);
    ok = doAddVersion(urlVersion, baseUrl + "macosx-cocoa-x86_64.tar.gz", MAC, X64);
    ok = doAddVersion(urlVersion, baseUrl + "macosx-cocoa-aarch64.tar.gz", MAC, ARM64);
    return ok;
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/downloads/packages/release";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://www.eclipse.org";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  public String mapVersion(String version) {

    // TODO remove this hack and get versions from reliable API
    return super.mapVersion(version.replace(" ", "-"));
  }

  @Override
  public String getCpeVendor() {
    return "eclipse";
  }

  @Override
  public String getCpeProduct() {
    return "eclipse";
  }
}
