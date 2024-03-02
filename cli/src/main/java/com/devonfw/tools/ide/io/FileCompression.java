package com.devonfw.tools.ide.io;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Enum to represent different file types which can be extracted using {@link FileAccess#extract(Path, Path, boolean)}
 * except for TAR files that are covered by {@link TarCompression}.
 */
enum FileCompression {
  /**
   * Common (Windows) ZIP archive format. Initially invented by PKZIP for MS-DOS and also famous from WinZIP software
   * for Windows.
   */
  ZIP,
  /** Java ARchive format. More or less a ZIP file. */
  JAR,
  /** Apple Disk iMaGe format. Similar to an ISO image. Commonly used for software releases on MacOS. */
  DMG,
  /**
   * MicroSoft Installer format. Commonly used for software releases on Windows that allow an installation wizard and
   * easy later uninstallation.
   */
  MSI,
  /**
   * Apple PacKaGe format. Internally a xar based archive with a specific structure. Used instead of {@link #DMG} if
   * additional changes have to be performed like drivers to be installed. Similar to what {@link #MSI} is on Windows.
   */
  PKG;

  public static FileCompression of(String extension) {

    String ext = extension.toUpperCase(Locale.ROOT);
    for (FileCompression compression : values()) {
      if (ext.equals(compression.name())) {
        return compression;
      }
    }
    return null;
  }
}