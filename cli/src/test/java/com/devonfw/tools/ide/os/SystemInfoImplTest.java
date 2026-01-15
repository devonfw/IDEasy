package com.devonfw.tools.ide.os;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link SystemInfoImpl}.
 */
class SystemInfoImplTest extends Assertions {

  /** Test {@link SystemInfoImpl#INSTANCE}. */
  @Test
  void testDefaultConstructor() {

    // arrange
    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");
    String architectureName = System.getProperty("os.arch");
    // act
    SystemInfo systemInfo = SystemInfoImpl.INSTANCE;
    // assert
    assertThat(systemInfo.getOsName()).isEqualTo(osName).isNotBlank();
    assertThat(systemInfo.getOsVersion().toString()).isEqualTo(osVersion).isNotBlank();
    assertThat(systemInfo.getArchitectureName()).isEqualTo(architectureName).isNotBlank();
  }

  /** Test {@link SystemInfoMock#WINDOWS_X64}. */
  @Test
  void testWindowsDetection() {

    // arrange
    OperatingSystem os = OperatingSystem.WINDOWS;
    SystemArchitecture arch = SystemArchitecture.X64;
    // act
    SystemInfo systemInfo = SystemInfoMock.WINDOWS_X64;
    // assert
    assertThat(systemInfo.getOs()).isSameAs(os);
    assertThat(systemInfo.getArchitecture()).isSameAs(arch);
    assertThat(systemInfo.toString()).isEqualTo("windows@x64(Windows 10[10.0]@amd64)");
  }

  /** Test {@link SystemInfoMock#MAC_X64}. */
  @Test
  void testMacDetection() {

    // arrange
    OperatingSystem os = OperatingSystem.MAC;
    SystemArchitecture arch = SystemArchitecture.X64;
    // act
    SystemInfo systemInfo = SystemInfoMock.MAC_X64;
    // assert
    assertThat(systemInfo.getOs()).isSameAs(os);
    assertThat(systemInfo.getArchitecture()).isSameAs(arch);
    assertThat(systemInfo.toString()).isEqualTo("mac@x64(Mac OS X[12.6.9]@x86_64)");
  }

  /** Test {@link SystemInfoMock#MAC_ARM64}. */
  @Test
  void testMacArmDetection() {

    // arrange
    OperatingSystem os = OperatingSystem.MAC;
    SystemArchitecture arch = SystemArchitecture.ARM64;
    // act
    SystemInfo systemInfo = SystemInfoMock.MAC_ARM64;
    // assert
    assertThat(systemInfo.getOs()).isSameAs(os);
    assertThat(systemInfo.getArchitecture()).isSameAs(arch);
    assertThat(systemInfo.toString()).isEqualTo("mac@arm64(Mac OS X[12.6.9]@aarch64)");
  }

  /** Test {@link SystemInfoMock#LINUX_X64}. */
  @Test
  void testLinuxDetection() {

    // arrange
    OperatingSystem os = OperatingSystem.LINUX;
    SystemArchitecture arch = SystemArchitecture.X64;
    // act
    SystemInfo systemInfo = SystemInfoMock.LINUX_X64;
    // assert
    assertThat(systemInfo.getOs()).isSameAs(os);
    assertThat(systemInfo.getArchitecture()).isSameAs(arch);
    assertThat(systemInfo.toString()).isEqualTo("linux@x64(Linux[3.13.0-74-generic]@x64)");
  }

}
