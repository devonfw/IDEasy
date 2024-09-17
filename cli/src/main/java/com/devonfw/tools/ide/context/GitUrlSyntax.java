package com.devonfw.tools.ide.context;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing the syntax of Git URLs, either SSH or HTTPS. Provides methods to format and convert Git URLs based on the syntax.
 */
public enum GitUrlSyntax {
  /**
   * The SSH Git URL syntax (e.g., git@github.com:user/repo.git).
   */
  SSH("git@"),

  /**
   * The HTTPS Git URL syntax (e.g., https://github.com/user/repo.git).
   */
  HTTPS("https://");

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
  public GitUrl format(GitUrl gitUrl) {
    String url = gitUrl.url();

    switch (this) {
      case SSH:
        if (url.startsWith(HTTPS.prefix)) {
          int index = url.indexOf("/", HTTPS.prefix.length());
          if (index > 0) {
            url = SSH.prefix + url.substring(HTTPS.prefix.length(), index) + ":" + url.substring(index + 1);
          }
        }
        break;
      case HTTPS:
        if (url.startsWith(SSH.prefix)) {
          int index = url.indexOf(":");
          if (index > 0) {
            url = HTTPS.prefix + url.substring(SSH.prefix.length(), index) + "/" + url.substring(index + 1);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported protocol: " + this);
    }

    return new GitUrl(url, gitUrl.branch());
  }

  /**
   * Converts the given {@link GitUrl} to the preferred protocol (either "ssh" or "https").
   * <p>
   * If the preferred protocol is not valid or the domain should not be converted, the original URL is returned.
   * </p>
   *
   * @param gitUrl the original {@link GitUrl} to be converted.
   * @param preferredProtocol the preferred protocol as a {@link String}, either "ssh" or "https".
   * @return the converted {@link GitUrl} or the original {@link GitUrl} if no conversion is required.
   * @throws IllegalArgumentException if the protocol is not supported.
   */
  public static GitUrl convertToPreferredProtocol(GitUrl gitUrl, String preferredProtocol) {
    if (preferredProtocol == null || preferredProtocol.isEmpty()) {
      return gitUrl; // No conversion, return the original GitUrl
    }

    String host = gitUrl.getHost();
    if (DOMAINS_WITH_NO_CONVERSION.contains(host.toLowerCase())) {
      return gitUrl;
    }

    GitUrlSyntax syntax;
    switch (preferredProtocol.toLowerCase()) {
      case "ssh":
        syntax = SSH;
        break;
      case "https":
        syntax = HTTPS;
        break;
      default:
        return gitUrl; // Unrecognized protocol, return the original GitUrl
    }

    return syntax.format(gitUrl);
  }
}
