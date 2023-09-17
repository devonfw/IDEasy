package com.devonfw.tools.ide.url.updater.kotlinc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Kotlin.
 */
public class KotlincUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "kotlinc";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        "https://github.com/JetBrains/kotlin/releases/download/v${version}/kotlin-compiler-${version}.zip");
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
