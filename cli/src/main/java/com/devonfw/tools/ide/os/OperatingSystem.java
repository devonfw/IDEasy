package com.devonfw.tools.ide.os;

import java.util.Locale;

/**
 * Enum with the supported operating systems.
 */
public enum OperatingSystem {
  /** Microsoft Windows. */
  WINDOWS("windows"),

  /** Apple MacOS (not iOS). */
  MAC("mac"),

  /** Linux (Ubunutu, Debian, SuSe, etc.) */
  LINUX("linux");

  private final String title;

  private OperatingSystem(String title) {

    this.title = title;
  }

  @Override
  public String toString() {

    return this.title;
  }

  /**
   * @param title the {@link #toString() string representation} of the requested {@link OperatingSystem}.
   * @return the according {@link OperatingSystem} or {@code null} if none matches.
   */
  public static OperatingSystem of(String title) {

    for (OperatingSystem os : values()) {
      if (os.title.equals(title)) {
        return os;
      }
    }
    return null;
  }

  public static OperatingSystem ofName(String osName) {

    String os = osName.toLowerCase(Locale.ROOT);
    if (os.startsWith("windows")) {
      return OperatingSystem.WINDOWS;
    } else if (os.startsWith("mac") || os.contains("darwin")) {
      return OperatingSystem.MAC;
    } else if (os.contains("linux")) {
      return OperatingSystem.LINUX;
    } else if (os.contains("bsd")) {
      return OperatingSystem.LINUX;
    } else if (os.contains("ix")) {
      return OperatingSystem.LINUX;
    } else {
      System.err.println("ERROR: Unknown operating system '" + osName + "'");
      // be tolerant: most of our users are working on windows
      // in case of an odd JVM or virtualization issue let us better continue than failing
      return OperatingSystem.WINDOWS;
    }
  }

  /**
   * @param suffix the file extension.
   * @return {@code true} if the given {@code suffix} is an executable file extension of this {@link OperatingSystem},
   * {@code false} otherwise.
   */
  public boolean isExecutable(String suffix) {

    if (suffix == null) {
      return false;
    }
    if (suffix.startsWith(".")) {
      suffix = suffix.substring(1);
    }
    if (this == WINDOWS) {
      if (suffix.equals("exe")) {
        return true;
      } else if (suffix.equals("msi")) {
        return true;
      } else if (suffix.equals("cmd")) {
        return true;
      } else if (suffix.equals("bat")) {
        return true;
      } else if (suffix.equals("ps1")) {
        return true;
      }
    } else {
      if (suffix.equals("sh")) {
        return true;
      } else if ((this == MAC) && suffix.equals("pkg")) {
        return true;
      }
    }
    return false;
  }

}
