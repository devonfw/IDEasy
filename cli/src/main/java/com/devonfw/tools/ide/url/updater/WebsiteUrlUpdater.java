package com.devonfw.tools.ide.url.updater;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An abstract class representing a website crawler that extends the AbstractCrawler class.
 * <p>
 * This class provides functionality for crawling a website and extracting the available version numbers by matching a
 * pattern in the website HTML body.
 * <p>
 * Concrete subclasses of this class must implement the getVersionPattern and getVersionUrl methods to specify the
 * version pattern and URL of the website to crawl, respectively.
 */
public abstract class WebsiteUrlUpdater extends AbstractUrlUpdater {

  /**
   * Retrieves the available versions by performing a GET request on the version URL and extracting the version numbers
   * from the HTML body of the response.
   *
   * @return a set of available version numbers
   */
  @Override
  protected Set<String> getVersions() {

    return doGetRegexMatchesAsList(doGetResponseBodyAsString(getVersionUrl()));
  }

  /**
   * Extracts the version numbers from the given HTML body by matching them against the pattern returned by the
   * getVersionPattern method.
   *
   * @param htmlBody the HTML body to extract version numbers from
   * @return a set of version numbers extracted from the HTML body
   */
  protected Set<String> doGetRegexMatchesAsList(String htmlBody) {

    Set<String> versions = new HashSet<>();
    var matcher = getVersionPattern().matcher(htmlBody);
    while (matcher.find()) {
      String version = matcher.group();
      addVersion(version, versions);
    }
    return versions;
  }

  /**
   * Returns the URL of the website to crawl.
   *
   * @return the URL of the website
   */
  protected abstract String getVersionUrl();

  /**
   * Returns the pattern for matching version numbers in the HTML body of the website.
   *
   * @return the pattern for matching version numbers
   */
  protected abstract Pattern getVersionPattern();

}
