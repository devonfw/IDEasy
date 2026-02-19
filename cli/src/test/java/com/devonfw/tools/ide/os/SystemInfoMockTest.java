package com.devonfw.tools.ide.os;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link SystemInfoMock}.
 */
class SystemInfoMockTest extends Assertions {

  /** Test {@link SystemInfoMock#of(String)} for windows. */
  @Test
  void testOfWindows() {

    // arrange
    String name = "windows";
    // act
    SystemInfo systemInfo = SystemInfoMock.of(name);
    // assert
    assertThat(systemInfo).isSameAs(SystemInfoMock.WINDOWS_X64);
  }

  /** Test {@link SystemInfoMock#of(String)} for mac. */
  @Test
  void testOfMac() {

    // arrange
    String name = "Mac";
    // act
    SystemInfo systemInfo = SystemInfoMock.of(name);
    // assert
    assertThat(systemInfo).isSameAs(SystemInfoMock.MAC_X64);
  }

  /** Test {@link SystemInfoMock#of(String)} for linux. */
  @Test
  void testOfLinux() {

    // arrange
    String name = "linux";
    // act
    SystemInfo systemInfo = SystemInfoMock.of(name);
    // assert
    assertThat(systemInfo).isSameAs(SystemInfoMock.LINUX_X64);
  }

}
