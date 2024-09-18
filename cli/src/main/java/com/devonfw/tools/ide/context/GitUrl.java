package com.devonfw.tools.ide.context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles parsing of git URLs.
 *
 * @param url the git url e.g. https://github.com/devonfw/ide-urls.git.
 * @param branch the branch name e.g. master.
 */
public record GitUrl(String url, String branch) {

  // Regex pattern to match SSH URLs
  private static final Pattern SSH_URL_PATTERN = Pattern.compile("git@([^:]+):([a-zA-Z0-9._/-]+)\\.git", Pattern.CASE_INSENSITIVE);


  /**
   * Checks if the given URL is a valid SSH URL.
   *
   * @return true if the URL is a valid SSH URL, false otherwise.
   */
  public boolean isSshUrl() {
    return SSH_URL_PATTERN.matcher(url).matches();
  }

  /**
   * Parses an SSH-style Git URL and extracts its components (host and repository path).
   *
   * @return a String array with the host and path, or null if the URL is not a valid SSH URL.
   */
  public String[] parseSshLink() {
    Matcher matcher = SSH_URL_PATTERN.matcher(url);
    if (matcher.matches()) {
      String host = matcher.group(1);  // Extracts the host (e.g., github.com)
      String path = matcher.group(2);  // Extracts the repository path (e.g., devonfw/IDEasy)
      return new String[] { host, path };
    } else {
      throw new RuntimeException("Invalid SSH URL: " + url);
    }
  }

  /**
   * Gets the host part of the URL, handling both SSH and HTTP(S) URLs.
   *
   * @return the host of the URL.
   * @throws RuntimeException if the URL is invalid.
   */
  public String getHost() {
    String[] sshComponents = getSshComponents();
    if (sshComponents != null) {
      return sshComponents[0];
    }
    // For HTTP/HTTPS URLs
    try {
      return new URL(url).getHost();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid HTTP/HTTPS URL: " + url, e);
    }
  }

  /**
   * Returns the path component of the Git URL.
   * <p>
   * Handles both SSH and HTTP(S) URLs, returning the repository path. Returns {@code null} if the URL is invalid or malformed.
   * </p>
   *
   * @return the path of the Git URL, or {@code null} if invalid.
   */
  public String getPath() {
    String[] sshComponents = getSshComponents();
    if (sshComponents != null) {
      return sshComponents[1];
    }
    // For HTTP/HTTPS URLs
    try {
      return new URL(url).getPath();
    } catch (MalformedURLException e) {
      return null;
    }
  }


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

  private String[] getSshComponents() {
    if (isSshUrl()) {
      return parseSshLink();
    }
    return null;
  }
}
