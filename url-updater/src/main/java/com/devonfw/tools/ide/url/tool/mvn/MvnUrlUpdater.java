package com.devonfw.tools.ide.url.tool.mvn;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for mvn (maven).
 */
public class MvnUrlUpdater extends AbstractMvnUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d\\.\\d)");

  @Override
  protected String getMvnVersionFolder() {

    return "maven-3";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

}
