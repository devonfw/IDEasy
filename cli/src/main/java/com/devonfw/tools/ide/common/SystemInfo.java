package com.devonfw.tools.ide.common;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface to get information about the current machine running the IDE.
 */
public interface SystemInfo {

  /**
   * @return the current {@link OperatingSystem}.
   */
  OperatingSystem getOs();

  /**
   * @return the raw name of the current {@link OperatingSystem}.
   */
  String getOsName();

  /**
   * @return the version of the current {@link OperatingSystem}.
   */
  VersionIdentifier getOsVersion();

  /**
   * @return the raw name of the current {@link SystemArchitecture}.
   */
  String getArchitectureName();

  /**
   * @return the current {@link SystemArchitecture}.
   */
  SystemArchitecture getArchitecture();

  /**
   * @return {@code true} if we are on {@link OperatingSystem#WINDOWS windows}.
   */
  default boolean isWindows() {

    return getOs() == OperatingSystem.WINDOWS;
  }

  /**
   * @return {@code true} if we are on {@link OperatingSystem#MAC mac OS}.
   */
  default boolean isMac() {

    return getOs() == OperatingSystem.MAC;
  }

  /**
   * @return {@code true} if we are on {@link OperatingSystem#LINUX linux}.
   */
  default boolean isLinux() {

    return getOs() == OperatingSystem.LINUX;
  }

}
