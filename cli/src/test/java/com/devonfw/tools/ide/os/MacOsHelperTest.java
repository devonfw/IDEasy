package com.devonfw.tools.ide.os;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Test of {@link MacOsHelper}.
 */
public class MacOsHelperTest extends AbstractIdeContextTest {

  private static final IdeContext CONTEXT = newContext(PROJECT_BASIC, null, false);

  private static final Path APPS_DIR = TEST_RESOURCES.resolve("mac-apps");

  /** Test "java" structure. */
  @Test
  public void testJava() {

    // arrange
    Path rootDir = APPS_DIR.resolve("java");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.MAC_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Contents/Resources/app"));
  }

  /** Test "special" structure. */
  @Test
  public void testSpecial() {

    // arrange
    Path rootDir = APPS_DIR.resolve("special");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.MAC_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isEqualTo(rootDir.resolve("Special.app/Contents/CorrectFolder"));
  }

  /** Test if OS is not Mac. */
  @Test
  public void testNotMac() {

    // arrange
    Path rootDir = APPS_DIR.resolve("java");
    MacOsHelper helper = new MacOsHelper(CONTEXT.getFileAccess(), SystemInformationMock.LINUX_X64, CONTEXT);
    // act
    Path linkDir = helper.findLinkDir(rootDir);
    // assert
    assertThat(linkDir).isSameAs(rootDir);
  }
}
