package com.devonfw.tools.ide.tool.obsidian;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link Obsidian}.
 */
class ObsidianTest extends AbstractIdeContextTest {

  /**
   * Test that the {@link Obsidian} commandlet is registered and properly classified.
   */
  @Test
  void testObsidianCommandletIsRegistered() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // assert
    assertThat(obsidian).isNotNull();
    assertThat(obsidian.getName()).isEqualTo("obsidian");
    assertThat(obsidian.getTags()).containsExactly(Tag.MARK_DOWN);
  }

  /**
   * Test that the Windows download is not extracted since it is an installer executable that has to be started.
   */
  @Test
  void testIsExtractIsFalseOnWindows() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isFalse();
  }

  /**
   * Test that the macOS download is extracted since the *.app has to be taken out of the DMG image.
   */
  @Test
  void testIsExtractIsTrueOnMac() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isTrue();
  }

  /**
   * Test that the Linux download is extracted since it is a tar.gz archive.
   */
  @Test
  void testIsExtractIsTrueOnLinux() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.isExtract()).isTrue();
  }

  /**
   * Test that the name used to find an existing installation in the Windows registry is not the lower-case tool name.
   */
  @Test
  void testGetWindowsRegistryAppName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Obsidian obsidian = context.getCommandletManager().getCommandlet(Obsidian.class);

    // act + assert
    assertThat(obsidian.getWindowsRegistryAppName()).isEqualTo("Obsidian");
  }
}
