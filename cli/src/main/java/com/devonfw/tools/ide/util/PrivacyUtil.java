package com.devonfw.tools.ide.util;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Utility for converting console output to GDPR compatible output.
 */
public class PrivacyUtil {

  private static final String USERNAME_PLACEHOLDER = "<username>";

  /**
   * Removes the username from a path string.
   *
   * @param text the String to adjust.
   * @return the adjusted String without the username.
   */
  public static String applyPrivacyToString(String text) {

    String separator = text.contains("\\") ? "\\" : "/";

    // Split the path using the detected separator
    String[] parts = text.split(separator.equals("\\") ? "\\\\" : separator);

    for (int i = 0; i < parts.length - 1; i++) {
      String current = parts[i].toLowerCase(Locale.ROOT);
      if ((current.equals("users") || current.equals("home")) && i + 1 < parts.length) {
        parts[i + 1] = USERNAME_PLACEHOLDER;
        break;
      }
    }

    // Rebuild the path using the original separator
    return String.join(separator, parts);
  }

  /**
   * Removes the username from a path.
   *
   * @param path the Path to adjust.
   * @return the adjusted String of the path without the username.
   */
  public static String applyPrivacyToPath(Path path) {
    return applyPrivacyToString(path.toString());
  }

}
