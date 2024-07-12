package com.devonfw.tools.ide.context;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handles parsing of git URLs.
 *
 * @param url the git url e.g. https://github.com/devonfw/ide-urls.git.
 * @param branch the branch name e.g. master.
 */
public record GitUrl(String url, String branch) {

  /**
   * Parses a git URL and omits the branch name if not provided.
   *
   * @return parsed URL.
   */
  public URL parseUrl() {

    String parsedUrl = this.url;
    if (this.branch != null && !this.branch.isEmpty()) {
      parsedUrl += "#" + this.branch;
    }
    URL validUrl;
    try {
      validUrl = new URL(parsedUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Git URL is not valid " + parsedUrl, e);
    }
    return validUrl;
  }
}
