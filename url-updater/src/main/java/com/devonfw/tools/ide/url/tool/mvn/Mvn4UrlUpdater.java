package com.devonfw.tools.ide.url.tool.mvn;

import java.util.regex.Pattern;

/**
 * {@link AbstractMvnUrlUpdater} for mvn (maven) versions 4.x.
 */
public class Mvn4UrlUpdater extends AbstractMvnUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d\\.\\d-[a-z]*?-\\d{1,2})");

  @Override
  protected String getMvnVersionFolder() {

    return "maven-4";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

}
