package com.devonfw.tools.ide.tool.java;

import com.devonfw.tools.ide.tool.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link JsonUrlUpdater} for Java.
 */
public class JavaUrlUpdater extends JsonUrlUpdater<JavaJsonObject, JavaJsonVersion> {

  private static final Logger logger = LoggerFactory.getLogger(NpmUrlUpdater.class);

  @Override
  protected String getTool() {

    return "java";
  }

  @Override
  protected String mapVersion(String version) {

    // remove the suffix "-LTS", this is necessary for java version 21+35-LTS
    if (version.endsWith("-LTS")) {
      version = version.substring(0, version.length() - 4);
    }
    return super.mapVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String mirror = "https://github.com/adoptium/temurin";
    String version = urlVersion.getName();
    int i = 0;
    int length = version.length();
    while (i < length) {
      char c = version.charAt(i);
      if (c >= '0' && c <= '9') {
        i++;
      } else {
        break;
      }
    }
    String major = version.substring(0, i);
    String code;
    if (version.charAt(0) == '8') {
      code = "jdk" + version.replace("b", "-b");
    } else {
      code = "jdk-" + version.replace("_", "%2B");
    }

    String baseUrl = mirror + major + "-binaries/releases/download/" + code + "/OpenJDK" + major;
    boolean success = doAddVersion(urlVersion, baseUrl + "U-jdk_x64_windows_hotspot_${version}.zip", WINDOWS);
    if (!success) {
      mirror = "https://github.com/AdoptOpenJDK/openjdk";
      baseUrl = mirror + major + "-binaries/releases/download/" + code + "/OpenJDK" + major;
      success = doAddVersion(urlVersion, baseUrl + "U-jdk_x64_windows_hotspot_${version}.zip", WINDOWS);
      if (!success) {
        return;
      }
    }
    doAddVersion(urlVersion, baseUrl + "U-jdk_x64_mac_hotspot_${version}.tar.gz", MAC);
    doAddVersion(urlVersion, baseUrl + "U-jdk_x64_linux_hotspot_${version}.tar.gz", LINUX);

  }

  @Override
  protected String doGetVersionUrl() {

    return "https://api.adoptium.net/v3/info/release_versions?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&page=0&page_size=50&project=jdk&release_type=ga&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse";
  }

  @Override
  protected Class<JavaJsonObject> getJsonObjectType() {

    return JavaJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(JavaJsonObject jsonItem, Collection<String> versions) {

    for (JavaJsonVersion item : jsonItem.getVersions()) {
      String version = item.getOpenjdkVersion();
      version = version.replace("+", "_");
      // replace 1.8.0_ to 8u
      if (version.startsWith("1.8.0_")) {
        version = "8u" + version.substring(6);
        version = version.replace("-b", "b");
      }
      addVersion(version, versions);
    }

  }

  @Override
  protected void collectVersionsWithDownloadsFromJson(JavaJsonObject jsonObj, UrlEdition edition) {

    Set<String> versions = new HashSet<>();

    for (JavaJsonVersion item : jsonObj.getVersions()) {

      String version = item.getOpenjdkVersion();
      version = version.replace("+", "_");
      // replace 1.8.0_ to 8u
      if (version.startsWith("1.8.0_")) {
        version = "8u" + version.substring(6);
        version = version.replace("-b", "b");
      }

      if (!addVersion(version, versions))
        continue;

      if (isTimeoutExpired()) {
        break;
      }

      UrlVersion urlVersion = edition.getChild(version);
      if (urlVersion == null || isMissingOs(urlVersion)) {
        try {
          urlVersion = edition.getOrCreateChild(version);
          addVersion(urlVersion);
          urlVersion.save();
        } catch (Exception e) {
          logger.error("For tool {} we failed to add version {}.", getToolWithEdition(), version, e);
        }
      }
    }
  }

  @Override
  protected Collection<JavaJsonVersion> getVersionItems(JavaJsonObject jsonObject) {
    //TODO
    throw new IllegalStateException();
  }

  @Override
  protected String getDownloadUrl(JavaJsonVersion jsonVersionItem) {
    //TODO
    throw new IllegalStateException();
  }

  @Override
  protected String getVersion(JavaJsonVersion jsonVersionItem) {
    //TODO
    throw new IllegalStateException();
  }
}
