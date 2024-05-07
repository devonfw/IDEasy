package com.devonfw.tools.ide.util;

import java.util.Locale;

/**
 * Utility class for filenames and extensions.
 */
public final class FilenameUtil {

  private FilenameUtil() {

    // construction forbidden
  }

  /**
   * @param path the file or path name to get the extension from.
   * @return the file extension excluding the dot from the given {@code path} or {@code null} if no extension is
   * present.
   */
  public static String getExtension(String path) {

    if (path == null) {
      return null;
    }
    path = path.toLowerCase(Locale.ROOT).replace('\\', '/');
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash < 0) {
      lastSlash = 0;
    }
    int lastDot = path.lastIndexOf('.');

    // workaround for sourceforge urls ending with /download like
    // https://sourceforge.net/projects/gcviewer/files/gcviewer-1.36.jar/download
    if (path.startsWith("https://") && path.contains("sourceforge") && path.endsWith("download")) {
      return path.substring(lastDot + 1, lastSlash);
    }

    if (lastDot < lastSlash) {
      return null;
    }
    // include previous ".tar" for ".tar.gz" or ".tar.bz2"
    int rawNameLength = lastDot - lastSlash;
    if ((rawNameLength > 4) && path.substring(lastDot - 4, lastDot).equals(".tar")) {
      lastDot = lastDot - 4;
    }
    return path.substring(lastDot + 1);
  }

}
