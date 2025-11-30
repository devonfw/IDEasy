package com.devonfw.tools.ide.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utility to remove private or sensitive information from log output.
 *
 * @see com.devonfw.tools.ide.context.IdeStartContext#isPrivacyMode()
 */
public final class PrivacyUtil {

  private static final Set<String> UNSENSITIVE_SEGMENTS = Set.of("project", "projects", "workspace", "workspaces", "conf", "settings", "software", "plugins",
      "setup", "update", "templates", "urls", "ide", "intellij", "eclipse", "vscode", "java", "mvn", "maven", "tmp", "backups", "backup", "bak", "src",
      "target", "main", "test", "master", "resource", "resources", "text", "txt", "less", "more", "com", "org", "javax", "groovy", "scala", "cpp", "common",
      "data", "doc", "documentation", "generated", "web", "public", "dataaccess", "persistence", "logic", "general", "git", "lock", "jpa", "tar", "tgz", "bz2",
      "tbz2", "zip", "compress", "compression", "global", "value", "code", "branch", "string", "long", "number", "numeric", "apache", "commons", "hibernate",
      "storage", "db", "spring", "springframework", "boot", "quarkus", "mnt", "usr", "user", "users", "windows", "etc", "var", "log", "lib", "drivers",
      "system", "system32", "appdata", "module", "info", "sha1", "md5", "sha256", "sha512", "pkcs", "p12", "cert", "file", "files", "bin", "bash", "program",
      "mingw64");

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
        boolean slash = isSlash(cp);
        if (slash) {
          cp = '/'; // normalize Windows backslash
        }
        if (slash || isSeparator(cp)) {
          appendSegment(arg, result, start, index);
          index += Character.charCount(cp);
          start = index;
          result.appendCodePoint(cp);
        } else if (!isFileSegment(cp)) {
          appendSegment(arg, result, start, index);
          start = index;
          break;
        } else {
          index += Character.charCount(cp);
        }
      }
      index = indexOfSlash(arg, index);
      if (index < 0) {
        result.append(arg, start, length); // append rest
      }
    }
    return result.toString();
  }

  private static int indexOfSlash(String arg, int start) {
    int index = arg.indexOf('/', start);
    int index2 = arg.indexOf('\\', start);
    if (index2 < 0) {
      return index;
    } else if ((index < 0) || (index2 < index)) {
      return index2;
    }
    return index;
  }

  private static void appendSegment(String arg, StringBuilder result, int start, int index) {

    String segment = arg.substring(start, index);
    if (UNSENSITIVE_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT)) || segment.length() <= 2) {
      result.append(segment);
    } else {
      result.repeat('*', segment.length());
    }
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
