package com.devonfw.tools.ide.os;

import java.util.Locale;
import java.util.Objects;

/**
 * TODO hohwille This type ...
 *
 */
public enum WindowsPathSyntax {

  /** Windows path like "C:\..." or "D:\...". */
  WINDOWS,

  /** MSys (git-bash) path like "/c/..." or "/d/...". */
  MSYS;

  /**
   * @param path the potential path. May be any {@link String}.
   * @return the the drive letter (e.g. "C" or "D") or {@code null} if the given {@link String} is not a path in this
   *         {@link WindowsPathSyntax}.
   */
  public String getDrive(String path) {

    if ((path != null) && (path.length() >= 3)) {
      char c0 = path.charAt(0);
      char c1 = path.charAt(1);
      char c2 = path.charAt(2);
      switch (this) {
        case WINDOWS: // 'C:\'
          if ((c1 == ':') && (c2 == '\\') && (c0 >= 'A') && (c0 <= 'Z')) {
            return Character.toString(c0);
          }
          break;
        case MSYS: // '/c/'
          if ((c0 == '/') && (c2 == '/') && isLowerLatinLetter(c1)) {
            return Character.toString(c1);
          }
          break;
        default:
          throw new IllegalArgumentException(toString());
      }
    }
    return null;
  }

  private static boolean isLowerLatinLetter(char c) {

    return (c >= 'a') && (c <= 'z');
  }

  /**
   * @param path the path where to replace the drive letter.
   * @param drive the new {@link #getDrive(String) drive letter}.
   * @return the new path pointing to the given {@code drive} in this {@link WindowsPathSyntax}.
   */
  public String replaceDrive(String path, String drive) {

    Objects.requireNonNull(path);
    Objects.requireNonNull(drive);
    if (path.length() < 3) {
      throw new IllegalArgumentException(path);
    }
    String restPath = path.substring(3);
    switch (this) {
      case WINDOWS:
        restPath = restPath.replace('/', '\\');
        break;
      case MSYS:
        restPath = restPath.replace('\\', '/');
        break;
      default:
        throw new IllegalStateException(toString());
    }
    return getRootPath(drive) + restPath;
  }

  /**
   * @param drive the drive letter (e.g. "C" or "D").
   * @return the root path for the given {@code drive} (e.g. "C:\\" or "/c/").
   */
  public String getRootPath(String drive) {

    Objects.requireNonNull(drive);
    if ((drive.length() != 1) || !isLowerLatinLetter(Character.toLowerCase(drive.charAt(0)))) {
      throw new IllegalArgumentException(drive);
    }
    switch (this) {
      case WINDOWS:
        return drive.toUpperCase(Locale.ROOT) + ":\\";
      case MSYS:
        return "/" + drive.toLowerCase(Locale.ROOT) + "/";
      default:
        throw new IllegalStateException(toString());
    }
  }

}
