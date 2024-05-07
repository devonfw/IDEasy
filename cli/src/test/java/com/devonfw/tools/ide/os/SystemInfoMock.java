package com.devonfw.tools.ide.os;

import java.util.List;
import java.util.Locale;

/**
 * Mock instances of {@link SystemInfo} to test OS specific behavior independent of the current OS running the test.
 */
public class SystemInfoMock {

  /** {@link OperatingSystem#WINDOWS} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo WINDOWS_X64 = new SystemInfoImpl("Windows 10", "10.0", "amd64");

  /** {@link OperatingSystem#MAC} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo MAC_X64 = new SystemInfoImpl("Mac OS X", "12.6.9", "x86_64");

  /** {@link OperatingSystem#MAC} with {@link SystemArchitecture#ARM64}. */
  public static final SystemInfo MAC_ARM64 = new SystemInfoImpl("Mac OS X", "12.6.9", "aarch64");

  /** {@link OperatingSystem#LINUX} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo LINUX_X64 = new SystemInfoImpl("Linux", "3.13.0-74-generic", "x64");

  private static final List<SystemInfo> MOCKS = List.of(WINDOWS_X64, MAC_X64, MAC_ARM64, LINUX_X64);

  public static SystemInfo of(String osString) {

    osString = osString.toLowerCase(Locale.ROOT);
    OperatingSystem os = OperatingSystem.of(osString);
    for (SystemInfo si : MOCKS) {
      if (os == null) {
        String osName = si.getOsName().toLowerCase(Locale.ROOT);
        if (osName.contains(osString)) {
          return si;
        }
      } else {
        if (si.getOs() == os) {
          return si;
        }
      }
    }
    throw new IllegalArgumentException("Unknown operating system: " + osString);
  }

}
