package com.devonfw.tools.ide.os;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Test of {@link MacOsHelper}.
 */
class MacOsHelperTest extends AbstractIdeContextTest {

  private static final IdeContext CONTEXT = newContext(PROJECT_BASIC, null, false);

  private static final Path APPS_DIR = TEST_RESOURCES.resolve("mac-apps");

  /** Test "java" structure. */
  @Test
  void testJava() {

    // arrange
    String tool = "java";
    Path rootDir = APPS_DIR.resolve(tool);
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.MAC_X64);
    // act
    Path linkDir = helper.findLinkDir(rootDir, tool);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Contents/Resources/app"));
  }

  /** Test "special" structure. */
  @Test
  void testSpecial() {

    // arrange
    String tool = "special";
    Path rootDir = APPS_DIR.resolve(tool);
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.MAC_X64);
    // act
    Path linkDir = helper.findLinkDir(rootDir, tool);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Special.app/Contents/CorrectFolder"));
  }

  /** Test if OS is not Mac. */
  @Test
  void testNotMac() {

    // arrange
    String tool = "java";
    Path rootDir = APPS_DIR.resolve(tool);
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.LINUX_X64);
    // act
    Path linkDir = helper.findLinkDir(rootDir, tool);
    // assert
    assertThat(linkDir).isSameAs(rootDir);
  }

  /** Test "java" structure. */
  @Test
  void testJmc() {

    // arrange
    String tool = "jmc";
    Path rootDir = APPS_DIR.resolve(tool);
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.MAC_X64);
    // act
    Path linkDir = helper.findLinkDir(rootDir, tool);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Contents/MacOS"));
  }

  /** Test that {@link MacOsHelper#findApplicationBundle(Path, String)} finds an existing application bundle. */
  @Test
  void testFindApplicationBundleFound() {

    // arrange
    Path appsDir = APPS_DIR.resolve("special");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.MAC_X64);
    // act
    Path appDir = helper.findApplicationBundle(appsDir, "Special");
    // assert
    assertThat(appDir).isEqualTo(appsDir.resolve("Special.app"));
  }

  /** Test that {@link MacOsHelper#findApplicationBundle(Path, String)} returns {@code null} when no such application bundle exists. */
  @Test
  void testFindApplicationBundleNotFound() {

    // arrange
    Path appsDir = APPS_DIR.resolve("special");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInfoMock.MAC_X64);
    // act
    Path appDir = helper.findApplicationBundle(appsDir, "DoesNotExist");
    // assert
    assertThat(appDir).isNull();
  }

}
