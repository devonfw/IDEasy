package com.devonfw.tools.ide.git;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing the syntax of Git URLs, either SSH or HTTPS. Provides methods to format and convert Git URLs based on the syntax.
 */
public enum GitUrlSyntax {

  /**
   * The DEFAULT Git URL syntax
   */
  DEFAULT("") {
    @Override
    public GitUrl format(GitUrl gitUrl) {
      return gitUrl; // No conversion for DEFAULT
    }
  },
  /**
   * The SSH Git URL syntax (e.g., git@github.com:user/repo.git).
   */
  SSH("git@") {
    @Override
    public GitUrl format(GitUrl gitUrl) {
      String url = gitUrl.url();
      if (isDomainWithNoConversion(url.toLowerCase())) {
        return gitUrl;
      }
      if (url.startsWith(HTTPS.prefix)) {
        int index = url.indexOf("/", HTTPS.prefix.length());
        if (index > 0) {
          url = SSH.prefix + url.substring(HTTPS.prefix.length(), index) + ":" + url.substring(index + 1);
        }
      }
      return new GitUrl(url, gitUrl.branch());
    }
  },

  /**
   * The HTTPS Git URL syntax (e.g., https://github.com/user/repo.git).
   */
  HTTPS("https://") {
    @Override
    public GitUrl format(GitUrl gitUrl) {
      String url = gitUrl.url();
      if (isDomainWithNoConversion(url.toLowerCase())) {
        return gitUrl;
      }
      if (url.startsWith(SSH.prefix)) {
        int index = url.indexOf(":");
        if (index > 0) {
          url = HTTPS.prefix + url.substring(SSH.prefix.length(), index) + "/" + url.substring(index + 1);
        }
      }
      return new GitUrl(url, gitUrl.branch());
    }
  };

  private final String prefix;

  private static final List<String> DOMAINS_WITH_NO_CONVERSION = Arrays.asList("github.com");

  GitUrlSyntax(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Formats the given Git URL according to the syntax represented by this enum constant.
   * <p>
   * Converts the URL between SSH and HTTPS formats. For example, an HTTPS URL can be converted to its corresponding SSH URL format, and vice versa.
   * </p>
   *
   * @param gitUrl the original {@link GitUrl} to be formatted.
   * @return the formatted {@link GitUrl} according to this syntax.
   * @throws IllegalArgumentException if the protocol is not supported.
   */
  public abstract GitUrl format(GitUrl gitUrl);

  private static boolean isDomainWithNoConversion(String url) {

    for (String domain : DOMAINS_WITH_NO_CONVERSION) {
      // Check if it's an HTTPS URL for the domain
      if (url.startsWith("https://" + domain + "/")) {
        return true;
      }

      // Check if it's an SSH URL for the domain
      if (url.startsWith("git@" + domain + ":")) {
        return true;
      }
    }

    return false;
  }


}
