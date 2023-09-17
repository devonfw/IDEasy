package com.devonfw.tools.ide.url.updater.kotlinc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for kotlinc-native.
 */
public class KotlincNativeUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "kotlinc-native";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = "https://github.com/JetBrains/kotlin/releases/download/v${version}/kotlin-native-";
    doAddVersion(urlVersion, baseUrl + "windows-x86_64-${version}.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "linux-x86_64-${version}.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "macos-x86_64-${version}.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "macos-aarch64-${version}.tar.gz", MAC, ARM64);
  }

  @Override
  protected String getVersionUrl() {

    return "https://api.github.com/repos/JetBrains/kotlin/releases";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");
  }
}
