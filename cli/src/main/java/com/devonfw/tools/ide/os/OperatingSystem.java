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
      System.err.println("ERROR: Unknown operating system '" + osName + "'"); // checkstyle:ignore SystemOut - static OS detection, no logger available
      // be tolerant: most of our users are working on windows
      // in case of an odd JVM or virtualization issue let us better continue than failing
      return OperatingSystem.WINDOWS;
    }
  }

}
