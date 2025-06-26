package com.devonfw.tools.ide.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utility to remove private or sensitive information from log output.
 *
 * @see com.devonfw.tools.ide.context.IdeStartContext#isPrivacyMode()
 */
public final class PrivacyUtil {

  private static final Set<String> UNSENSITIVE_SEGMENTS = Set.of("project", "projects", "ide", "src", "target", "main", "test", "master", "java", "resource",
      "resources", "text", "txt", "less", "more", "com", "org", "javax", "groovy", "scala", "cpp", "common", "data", "doc", "documentation",
      "generated", "web", "public", "dataaccess", "persistence", "logic", "general", "git", "lock", "jpa", "tar", "tgz", "bz2", "tbz2", "zip", "compress",
      "compression", "global", "value", "code", "branch", "string", "long", "number", "numeric", "apache", "commons", "hibernate", "storage", "db", "spring",
      "springframework", "boot", "quarkus", "mnt", "usr", "user", "users", "windows", "etc", "var", "log", "lib", "driver", "system", "system32", "appdata",
      "module", "info", "sha1", "md5", "sha256", "sha512", "pkcs", "p12", "cert");

  // construction forbidden
  private PrivacyUtil() {

  }

  /**
   * @param arg the path or any {@link String} containing one or multiple paths.
   * @return a normalized form of the
   */
  public static String removeSensitivePathInformation(String arg) {
    int index = indexOfSlash(arg, 0);
    if (index < 0) {
      return arg;
    }
    int length = arg.length();
    StringBuilder result = new StringBuilder(length);
    int start = 0;
    while (index >= 0) {
      // index is pointing to the first slash of an absolute or relative path, we first search backwards from start to index to find a potential starting folder-name
      int i = start;
      int folderStart = start;
      while (i < index) {
        int cp = arg.codePointAt(i);
        i += Character.charCount(cp);
        if (!isFileSegment(cp) && !isSeparator(cp)) {
          folderStart = i;
        }
      }
      result.append(arg, start, folderStart);
      start = folderStart;
      index = folderStart;
      // now we remove sensitive information from the current path
      while (index < length) {
        int cp = arg.codePointAt(index);
        index += Character.charCount(cp);
        boolean slash = isSlash(cp);
        if (slash) {
          cp = '/'; // normalize Windows backslash
        }
        if (slash || isSeparator(cp)) {
          appendSegment(arg, result, start, index - 1);
          start = index;
          result.appendCodePoint(cp);
        } else if (!isFileSegment(cp)) {
          index -= Character.charCount(cp);
          appendSegment(arg, result, start, index);
          start = index;
          break;
        }
      }
      start = index;
      index = indexOfSlash(arg, index);
      if (index < 0) {
        result.append(arg, start, length); // append rest
      }
    }
    return result.toString();
  }

  private static int indexOfSlash(String arg, int start) {
    int index = arg.indexOf('/', start);
    if (index < 0) {
      index = arg.indexOf('\\', start);
    }
    return index;
  }

  private static int appendSegment(String arg, StringBuilder result, int start, int index) {

    String segment = arg.substring(start, index);
    if (UNSENSITIVE_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT)) || segment.length() <= 2) {
      result.append(segment);
    } else {
      result.repeat('*', segment.length());
    }
    return index;
  }

  private static boolean isSlash(int cp) {
    return (cp == '/') || (cp == '\\');
  }

  private static boolean isSeparator(int cp) {
    return (cp == '.') || (cp == '-') || (cp == '_');
  }

  private static boolean isFileSegment(int cp) {

    return Character.isLetter(cp) || Character.isDigit(cp);
  }


}
