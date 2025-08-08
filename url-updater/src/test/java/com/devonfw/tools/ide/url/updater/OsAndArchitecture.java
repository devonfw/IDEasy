package com.devonfw.tools.ide.url.updater;

import java.util.List;

/**
 * Enum representing different combinations of operating systems and architectures for URL downloads.
 *
 * @see com.devonfw.tools.ide.os.OperatingSystem
 * @see com.devonfw.tools.ide.os.SystemArchitecture
 */
public enum OsAndArchitecture {

  /** OS and architecture agnostic. */
  AGNOSTIC(""),

  /** OS specific, architecture agnostic. */
  OS("windows", "mac", "linux"),

  /** all OS with x64 architecture. */
  OS_X64("windows_x64", "mac_x64", "linux_x64"),

  /** all OS with x64 architecture amd Mac arm64. */
  OS_X64_MAC_ARM("windows_x64", "mac_x64", "mac_arm64", "linux_x64"),

  /** all OS with their standard architecture (arm64 on mac). */
  OS_DEF_ARCH("windows_x64", "mac_arm64", "linux_x64"),

  /** all OS and architecture combinations. */
  OS_ARCH("windows_x64", "windows_arm64", "mac_x64", "mac_arm64", "linux_x64", "linux_arm64");

  private final List<String> filenames;

  private OsAndArchitecture(String... filenames) {
    this.filenames = List.of(filenames);
  }

  /**
   * @return the {@link List} of filenames for {@link com.devonfw.tools.ide.url.model.file.UrlDownloadFile}s.
   */
  public List<String> getFilenames() {

    return this.filenames;
  }
}
