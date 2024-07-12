package com.devonfw.tools.ide.os;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Syntax of an absolute {@link Path} on Windows. The standard syntax is obviously {@link #WINDOWS}, however there is also {@link #MSYS} for shells based on
 * MSYS like the famous git-bash that uses a linux compatible path syntax.
 */
public enum WindowsPathSyntax {

  /**
   * Windows path like "C:\..." or "D:\...".
   */
  WINDOWS('\\'),

  /**
   * MSys (git-bash) path like "/c/..." or "/d/...".
   */
  MSYS('/');

  private final char separator;

  private WindowsPathSyntax(char separator) {
    this.separator = separator;
  }

  /**
   * @param path the potential path. May be any {@link String}.
   * @return the the drive letter (e.g. "C" or "D") or {@code null} if the given {@link String} is not a path in this {@link WindowsPathSyntax}.
   */
  public String getDrive(String path) {

    if ((path != null) && (path.length() >= 3)) {
      char c0 = path.charAt(0);
      char c1 = path.charAt(1);
      char c2 = path.charAt(2);
      switch (this) {
        case WINDOWS: // 'C:\'
          if ((c1 == ':') && isSlash(c2) && (c0 >= 'A') && (c0 <= 'Z')) {
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

  private static boolean isSlash(char c2) {
    // on Windows both slash (/) and backslash (\) are acceptable as folder separator in a Path
    // While strict Windows syntax is to use backslash we are tolerant here so our Windows users can also configure
    // a path in ide.properties using slashes since backslash is the escape character in Java properties files and then
    // users have to use double backslash.
    return (c2 == '\\') || (c2 == '/');
  }

  private static boolean isLowerLatinLetter(char c) {

    return (c >= 'a') && (c <= 'z');
  }

  /**
   * @param path the {@link Path} to format.
   * @return the given {@link Path} formatted as {@link String} to this {@link WindowsPathSyntax}.
   */
  public String format(Path path) {

    if (path == null) {
      return null;
    }
    int nameCount = path.getNameCount();
    StringBuilder sb = new StringBuilder(nameCount * 6);
    int start = formatRootPath(path, sb);
    for (int i = start; i < nameCount; i++) {
      if (i > 0) {
        sb.append(this.separator);
      }
      Path segment = path.getName(i);
      sb.append(segment);
    }
    return sb.toString();
  }

  private int formatRootPath(Path path, StringBuilder sb) {

    Path root = path.getRoot();
    if (root == null) {
      return 0;
    }
    String rootString = root.toString();
    int length = rootString.length();
    if ((length == 3) && (rootString.charAt(1) == ':') && (rootString.charAt(2) == '\\')) {
      // so we have a WINDOWS driver letter as root
      char drive = Character.toLowerCase(rootString.charAt(0));
      if (isLowerLatinLetter(drive)) {
        if (this == MSYS) {
          sb.append(this.separator);
          sb.append(drive);
          sb.append(this.separator);
        } else {
          sb.append(rootString); // nothing to convert from WINDOWS to WINDOWS
        }
        return 0;
      }
    }
    if ((length == 1) && (this == WINDOWS)) {
      if (path.getNameCount() > 0) {
        root = path.getName(0);
        String drive = root.toString();
        if (drive.length() == 1) {
          char c = drive.charAt(0);
          if ((isLowerLatinLetter(c))) {
            // so we have a path starting with a driver letter in MSYS syntax (e.g. "/c/") but want WINDOWS syntax
            sb.append(Character.toUpperCase(c));
            sb.append(':');
            return 1;
          }
        }
      }
    }
    sb.append(this.separator);
    if (length > 1) {
      // this should actually never happen and only exists for robustness in odd edge-cases
      sb.append(rootString, 1, length);
      sb.append(this.separator);
    }
    return 0;
  }

  /**
   * @param path the path where to replace the drive letter.
   * @param drive the new {@link #getDrive(String) drive letter}.
   * @return the new path pointing to the given {@code drive} in this {@link WindowsPathSyntax}.
   */
  public String normalize(String path, String drive) {

    Objects.requireNonNull(path);
    Objects.requireNonNull(drive);
    if (path.length() < 3) {
      throw new IllegalArgumentException(path);
    }
    String restPath = path.substring(3);
    restPath = switch (this) {
      case WINDOWS -> restPath.replace('/', '\\');
      case MSYS -> restPath.replace('\\', '/');
      default -> throw new IllegalStateException(toString());
    };
    return getRootPath(drive) + restPath;
  }

  public String normalize(String value) {

    String drive = WindowsPathSyntax.WINDOWS.getDrive(value);
    if (drive == null) {
      drive = WindowsPathSyntax.MSYS.getDrive(value);
    }
    if (drive != null) {
      value = normalize(value, drive);
    }
    return value;
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
    return switch (this) {
      case WINDOWS -> drive.toUpperCase(Locale.ROOT) + ":\\";
      case MSYS -> "/" + drive.toLowerCase(Locale.ROOT) + "/";
      default -> throw new IllegalStateException(toString());
    };
  }

  /**
   * Normalizes a {@link String} that may be an absolute Windows {@link Path}.
   *
   * @param value the {@link String} to normalize.
   * @param bash - {@code true} to convert paths to {@link #MSYS} (git-bash), {@code false} for {@link #WINDOWS}.
   * @return the given {@code value} that was normalized if it has been an absolute Windows {@link Path}.
   */
  public static String normalize(String value, boolean bash) {

    WindowsPathSyntax targetSyntax;
    if (bash) {
      targetSyntax = WindowsPathSyntax.MSYS;
    } else {
      targetSyntax = WindowsPathSyntax.WINDOWS;
    }
    return targetSyntax.normalize(value);
  }
}
