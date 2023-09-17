package com.devonfw.tools.ide.common;

/**
 * Mock instances of {@link SystemInfo} to test OS specific behavior independent of the current OS running the test.
 */
public class SystemInformationMock {

  /** {@link OperatingSystem#WINDOWS} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo WINDOWS_X64 = new SystemInfoImpl("Windows 10", "10.0", "amd64");

  /** {@link OperatingSystem#MAC} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo MAC_X64 = new SystemInfoImpl("Mac OS X", "12.6.9", "x86_64");

  /** {@link OperatingSystem#MAC} with {@link SystemArchitecture#ARM64}. */
  public static final SystemInfo MAC_ARM64 = new SystemInfoImpl("Mac OS X", "12.6.9", "aarch64");

  /** {@link OperatingSystem#LINUX} with {@link SystemArchitecture#X64}. */
  public static final SystemInfo LINUX_X64 = new SystemInfoImpl("Linux", "3.13.0-74-generic", "x64");

}
